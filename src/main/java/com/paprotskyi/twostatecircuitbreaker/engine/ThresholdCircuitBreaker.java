package com.paprotskyi.twostatecircuitbreaker.engine;

import com.paprotskyi.twostatecircuitbreaker.exception.IncorrectStateLogicException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.ResultRecordedAsFailureException;
import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.core.lang.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;

@Slf4j
public class ThresholdCircuitBreaker implements CircuitBreaker {

  private final String name;
  private final AtomicReference<SimpleState> stateReference;
  private final CircuitBreakerConfig circuitBreakerConfig;
  private final StateTransitionCalculator stateTransitionCalculator;
  private final Clock clock;
  private final Function<Clock, Long> currentTimestampFunction;
  private final Map<String, String> tags;
  private final TimeUnit timestampUnit;

  public ThresholdCircuitBreaker(String name,
                                 Clock clock,
                                 CircuitBreakerConfig circuitBreakerConfig) {
    this.name = name;
    this.circuitBreakerConfig = Objects
        .requireNonNull(circuitBreakerConfig, "Config must not be null");
    this.clock = clock;
    this.currentTimestampFunction = circuitBreakerConfig.getCurrentTimestampFunction();
    this.stateReference = new AtomicReference<>(new ClosedState());
    this.timestampUnit = circuitBreakerConfig.getTimestampUnit();
    this.tags = Collections.emptyMap();
    this.stateTransitionCalculator = new StateTransitionCalculator();
  }

  public ThresholdCircuitBreaker(String name) {
    this(name, Clock.systemUTC(), CircuitBreakerConfig.ofDefaults());
  }

  @Override
  public boolean tryAcquirePermission() {
    boolean callPermitted = stateReference.get().tryAcquirePermission();
    log.info("CircuitBreaker call is {}permitted, state: {}", callPermitted ? "" : "not ", getState());
    return callPermitted;
  }

  @Override
  public void releasePermission() {
    stateReference.get().releasePermission();
  }

  @Override
  public void acquirePermission() {
    stateReference.get().acquirePermission();
  }

  @Override
  public void onError(long duration, TimeUnit durationUnit, Throwable throwable) {
    // Handle the case if the completable future throws a CompletionException wrapping the original exception
    // where original exception is the one to retry not the CompletionException.
    if (throwable instanceof CompletionException || throwable instanceof ExecutionException) {
      Throwable cause = throwable.getCause();
      handleThrowable(duration, durationUnit, cause);
    } else {
      handleThrowable(duration, durationUnit, throwable);
    }
  }

  @Override
  public void onSuccess(long duration, TimeUnit durationUnit) {
    log.info("ThresholdCircuitBreaker '{}' succeeded:", name);
    stateReference.get().onSuccess(duration, durationUnit);
  }

  @Override
  public void onResult(long duration, TimeUnit durationUnit, @Nullable Object result) {
    if (result != null && circuitBreakerConfig.getRecordResultPredicate().test(result)) {
      log.info("ThresholdCircuitBreaker '{}' recorded a result type '{}' as failure:", name, result.getClass());
      ResultRecordedAsFailureException failure = new ResultRecordedAsFailureException(name, result);
      stateReference.get().onError(duration, durationUnit, failure);
    } else {
      onSuccess(duration, durationUnit);
      if (result != null) {
        handlePossibleTransition(Either.left(result));
      }
    }
  }

  private void handleThrowable(long duration, TimeUnit durationUnit, Throwable throwable) {
    if (circuitBreakerConfig.getIgnoreExceptionPredicate().test(throwable)) {
      log.info("CircuitBreaker '{}' ignored an exception:", name, throwable);
      releasePermission();
      return;
    }
    if (circuitBreakerConfig.getRecordExceptionPredicate().test(throwable)) {
      log.info("CircuitBreaker '{}' recorded an exception as failure:", name, throwable);
      stateReference.get().onError(duration, durationUnit, throwable);
    } else {
      log.info("CircuitBreaker '{}' recorded an exception as success:", name, throwable);
      stateReference.get().onSuccess(duration, durationUnit);
    }
    handlePossibleTransition(Either.right(throwable));
  }

  private void handlePossibleTransition(Either<Object, Throwable> result) {
    CircuitBreakerConfig.TransitionCheckResult transitionCheckResult = circuitBreakerConfig.getTransitionOnResult()
        .apply(result);
    stateReference.get().handlePossibleTransition(transitionCheckResult);
  }

  @Override
  public void reset() {
    log.error("CircuitBreaker {} State reset to CLOSED state", getName());
    stateReference.getAndUpdate(currentState -> new ClosedState());
  }

  @Override
  public void transitionToClosedState() {
    stateTransition(State.CLOSED, currentState -> new ClosedState(currentState.getMetrics()));
  }

  @Override
  public void transitionToOpenState() {
    stateTransition(OPEN, currentState -> new OpenState(currentState.getMetrics()));
  }

  @Override
  public void transitionToOpenStateFor(Duration waitDuration) {
    //currently, won't be used
  }

  @Override
  public void transitionToOpenStateUntil(Instant waitUntil) {
    //currently, won't be used
  }

  @Override
  public void transitionToHalfOpenState() {
    throw new IncorrectStateLogicException(
        "Transition to half-open state must not be initiated in ThresholdCircuitBreaker");
  }

  @Override
  public void transitionToDisabledState() {
    throw new IncorrectStateLogicException(
        "Transition to disabled state must not be initiated in ThresholdCircuitBreaker");
  }

  @Override
  public void transitionToMetricsOnlyState() {
    throw new IncorrectStateLogicException(
        "Transition to metrics only state must not be initiated in ThresholdCircuitBreaker");
  }

  @Override
  public void transitionToForcedOpenState() {
    throw new IncorrectStateLogicException(
        "Transition to forced open state must not be initiated in ThresholdCircuitBreaker");
  }

  private void stateTransition(State newState,
                               UnaryOperator<SimpleState> newStateGenerator) {
    log.info("CircuitBreaker {} transition to {} state", getName(), newState.name());
    stateReference.getAndUpdate(currentState -> {
      StateTransition.transitionBetween(getName(), currentState.getState(), newState);
      return newStateGenerator.apply(currentState);
    });
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public State getState() {
    return stateReference.get().getState();
  }

  @Override
  public CircuitBreakerConfig getCircuitBreakerConfig() {
    return circuitBreakerConfig;
  }

  @Override
  public Metrics getMetrics() {
    return stateReference.get().getMetrics();
  }

  @Override
  public Map<String, String> getTags() {
    return tags;
  }

  @Override
  public EventPublisher getEventPublisher() {
    throw new IncorrectStateLogicException("Even publisher is not used in probability implementation");
  }

  @Override
  public long getCurrentTimestamp() {
    return this.currentTimestampFunction.apply(clock);
  }

  @Override
  public TimeUnit getTimestampUnit() {
    return timestampUnit;
  }

  private interface SimpleState {

    CircuitBreaker.State getState();

    SimpleMetrics getMetrics();

    boolean tryAcquirePermission();

    void acquirePermission();

    void releasePermission();

    void handlePossibleTransition(CircuitBreakerConfig.TransitionCheckResult result);

    void onError(long duration, TimeUnit durationUnit, Throwable throwable);

    void onSuccess(long duration, TimeUnit durationUnit);
  }

  private class ClosedState implements SimpleState {

    private final SimpleMetrics circuitBreakerMetrics;
    private final AtomicBoolean isClosed;

    public ClosedState() {
      this.circuitBreakerMetrics = SimpleMetrics.forClosed(getCircuitBreakerConfig());
      this.isClosed = new AtomicBoolean(true);
    }

    public ClosedState(SimpleMetrics metrics) {
      this.circuitBreakerMetrics = metrics;
      this.isClosed = new AtomicBoolean(true);
    }

    @Override
    public State getState() {
      return State.CLOSED;
    }

    @Override
    public SimpleMetrics getMetrics() {
      return circuitBreakerMetrics;
    }

    @Override
    public boolean tryAcquirePermission() {
      return isClosed.get();
    }

    @Override
    public void acquirePermission() {
      //noOp
    }

    @Override
    public void releasePermission() {
      //noOp
    }

    @Override
    public void handlePossibleTransition(CircuitBreakerConfig.TransitionCheckResult result) {
      if (result.isTransitionToOpen()) {
        toOpenState();
      }
    }

    private synchronized void toOpenState() {
      if (isClosed.compareAndSet(true, false)) {
        transitionToOpenState();
      }
    }

    @Override
    public void onError(long duration, TimeUnit durationUnit, Throwable throwable) {
      checkIfThresholdsExceeded(circuitBreakerMetrics.onError(duration, durationUnit));
    }

    @Override
    public void onSuccess(long duration, TimeUnit durationUnit) {
      checkIfThresholdsExceeded(circuitBreakerMetrics.onSuccess(duration, durationUnit));
    }

    private void checkIfThresholdsExceeded(SimpleMetrics.Result result) {
      if (SimpleMetrics.Result.hasExceededThresholds(result) && isClosed.compareAndSet(true, false)) {
        transitionToOpenState();
      }
    }
  }

  private class OpenState implements SimpleState {

    private final SimpleMetrics circuitBreakerMetrics;

    private final AtomicBoolean isOpen;

    private final long openStateTransitionTimestamp;

    private static final float DEFAULT_TRANSITION_RATING_THRESHOLD = 0.4f;

    public OpenState(SimpleMetrics circuitBreakerMetrics) {
      this.circuitBreakerMetrics = circuitBreakerMetrics;
      this.isOpen = new AtomicBoolean(true);
      this.openStateTransitionTimestamp = getCurrentTimestamp();
    }

    @Override
    public State getState() {
      return State.OPEN;
    }

    @Override
    public SimpleMetrics getMetrics() {
      return circuitBreakerMetrics;
    }

    @Override
    public boolean tryAcquirePermission() {
      // get the transitioning rating from OPEN to CLOSED state and compare it with the threshold
      if (isOpen.get()) {
        float toClosedTransitionRating = calculateTransitionRatingValue();
        log.info("Calculated transition rating {}", toClosedTransitionRating);
        if (toClosedTransitionRating >= DEFAULT_TRANSITION_RATING_THRESHOLD) {
          toClosedState();
          return true;
        }
        log.debug("Declining the request, because the state is still OPEN");
        circuitBreakerMetrics.onCallNotPermitted();
        return false;
      }
      return true;
    }

    private synchronized void toClosedState() {
      if (isOpen.compareAndSet(true, false)) {
        transitionToClosedState();
      }
    }

    private float calculateTransitionRatingValue() {
      // Calculate the probability of transitioning to the Closed state
      long currentOpenStateDuration = getCurrentTimestamp() - openStateTransitionTimestamp;
      log.debug("Current open state duration in nanos: {}", currentOpenStateDuration);
      return stateTransitionCalculator.calculateTransitionValue(circuitBreakerMetrics, currentOpenStateDuration);
    }

    @Override
    public void acquirePermission() {
      if (!tryAcquirePermission()) {
        throw CallNotPermittedException
            .createCallNotPermittedException(ThresholdCircuitBreaker.this);
      }
    }

    @Override
    public void releasePermission() {
      throw new IncorrectStateLogicException(
          "Permission release must not be initiated in ThresholdCircuitBreaker");
    }

    @Override
    public void handlePossibleTransition(CircuitBreakerConfig.TransitionCheckResult result) {
      // noOp
    }

    @Override
    public void onError(long duration, TimeUnit durationUnit, Throwable throwable) {
      circuitBreakerMetrics.onError(duration, durationUnit);
    }

    @Override
    public void onSuccess(long duration, TimeUnit durationUnit) {
      circuitBreakerMetrics.onSuccess(duration, durationUnit);
    }
  }
}
