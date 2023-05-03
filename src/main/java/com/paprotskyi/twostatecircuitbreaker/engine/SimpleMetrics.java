package com.paprotskyi.twostatecircuitbreaker.engine;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.metrics.FixedSizeSlidingWindowMetrics;
import io.github.resilience4j.core.metrics.Metrics;
import io.github.resilience4j.core.metrics.Snapshot;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * This class almost fully reuses the Resilience4j metrics implementation,
 * which is package-private in the library.
 * Please see the io.github.resilience4j.circuitbreaker.internal.CircuitBreakerMetrics class implementation.
 */
public class SimpleMetrics implements CircuitBreaker.Metrics {

  private final FixedSizeSlidingWindowMetrics metrics;
  private final float failureRateThreshold;
  private final float slowCallRateThreshold;
  private final long slowCallDurationThresholdInNanos;
  private final LongAdder numberOfNotPermittedCalls;
  private final int minimumNumberOfCalls;

  /*
   * This class implements only the COUNT_BASED sliding window type metrics
   */
  public SimpleMetrics(int slidingWindowSize,
                       CircuitBreakerConfig circuitBreakerConfig) {
    this.metrics = new FixedSizeSlidingWindowMetrics(slidingWindowSize);
    this.minimumNumberOfCalls = Math
        .min(circuitBreakerConfig.getMinimumNumberOfCalls(), slidingWindowSize);

    this.failureRateThreshold = circuitBreakerConfig.getFailureRateThreshold();
    this.slowCallRateThreshold = circuitBreakerConfig.getSlowCallRateThreshold();
    this.slowCallDurationThresholdInNanos = circuitBreakerConfig.getSlowCallDurationThreshold()
        .toNanos();
    this.numberOfNotPermittedCalls = new LongAdder();
  }

  static SimpleMetrics forClosed(CircuitBreakerConfig circuitBreakerConfig) {
    return new SimpleMetrics(
        circuitBreakerConfig.getSlidingWindowSize(),
        circuitBreakerConfig);
  }

  /**
   * Records a call which was not permitted, because the CircuitBreaker state is OPEN.
   */
  void onCallNotPermitted() {
    numberOfNotPermittedCalls.increment();
  }

  /**
   * Records a successful call and checks if the thresholds are exceeded.
   *
   * @return the result of the check
   */
  public Result onSuccess(long duration, TimeUnit durationUnit) {
    Snapshot snapshot;
    if (durationUnit.toNanos(duration) > slowCallDurationThresholdInNanos) {
      snapshot = metrics.record(duration, durationUnit, Metrics.Outcome.SLOW_SUCCESS);
    } else {
      snapshot = metrics.record(duration, durationUnit, Metrics.Outcome.SUCCESS);
    }
    return checkIfThresholdsExceeded(snapshot);
  }

  /**
   * Records a failed call and checks if the thresholds are exceeded.
   *
   * @return the result of the check
   */
  public Result onError(long duration, TimeUnit durationUnit) {
    Snapshot snapshot;
    if (durationUnit.toNanos(duration) > slowCallDurationThresholdInNanos) {
      snapshot = metrics.record(duration, durationUnit, Metrics.Outcome.SLOW_ERROR);
    } else {
      snapshot = metrics.record(duration, durationUnit, Metrics.Outcome.ERROR);
    }
    return checkIfThresholdsExceeded(snapshot);
  }

  /**
   * Checks if the failure rate is above the threshold or if the slow calls percentage is above
   * the threshold.
   *
   * @param snapshot a metrics snapshot
   * @return false, if the thresholds haven't been exceeded.
   */
  private Result checkIfThresholdsExceeded(Snapshot snapshot) {
    float failureRateInPercentage = getFailureRate(snapshot);
    float slowCallsInPercentage = getSlowCallRate(snapshot);

    if (failureRateInPercentage == -1 || slowCallsInPercentage == -1) {
      return Result.BELOW_MINIMUM_CALLS_THRESHOLD;
    }
    if (failureRateInPercentage >= failureRateThreshold
        && slowCallsInPercentage >= slowCallRateThreshold) {
      return Result.ABOVE_THRESHOLDS;
    }
    if (failureRateInPercentage >= failureRateThreshold) {
      return Result.FAILURE_RATE_ABOVE_THRESHOLDS;
    }

    if (slowCallsInPercentage >= slowCallRateThreshold) {
      return Result.SLOW_CALL_RATE_ABOVE_THRESHOLDS;
    }
    return Result.BELOW_THRESHOLDS;
  }

  private boolean checkNotExceedsMinimumNumberOfCalls(Snapshot snapshot) {
    int bufferedCalls = snapshot.getTotalNumberOfCalls();
    return bufferedCalls == 0 || bufferedCalls < minimumNumberOfCalls;
  }

  private float getSlowCallRate(Snapshot snapshot) {
    if (checkNotExceedsMinimumNumberOfCalls(snapshot)) {
      return -1.0f;
    }
    return snapshot.getSlowCallRate();
  }

  private float getFailureRate(Snapshot snapshot) {
    if (checkNotExceedsMinimumNumberOfCalls(snapshot)) {
      return -1.0f;
    }
    return snapshot.getFailureRate();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float getFailureRate() {
    return getFailureRate(metrics.getSnapshot());
  }

  private float getTargetCallNumberRate(Snapshot snapshot, int targetMetrics) {
    if (checkNotExceedsMinimumNumberOfCalls(snapshot)) {
      return 0.0f;
    }
    int totalCalls = snapshot.getTotalNumberOfCalls();

    float targetRateDecimal = (float) targetMetrics / totalCalls;
    if (targetRateDecimal == Float.POSITIVE_INFINITY || Float.isNaN(targetRateDecimal)) {
      return 0.0f;
    }
    return targetRateDecimal;
  }

  /**
   * NOTE: This method returns 0.0f in case the total number of measured calls
   * is less than minimumNumberOrCalls. This can be used in the probability formula.
   * This works not in the same way as an original implementation.
   */
  public float getDecimalFailureRate() {
    Snapshot snapshot = metrics.getSnapshot();
    return getTargetCallNumberRate(snapshot, snapshot.getNumberOfFailedCalls());
  }

  /**
   * NOTE: This method returns 0.0f in case the total number of measured calls
   * is less than minimumNumberOrCalls. This can be used in the probability formula.
   * This works not in the same way as an original implementation.
   */
  //TODO this can be also considered as two parts in probabilities: successful and failed slow calls
  public float getDecimalSlowCallRate() {
    Snapshot snapshot = metrics.getSnapshot();
    return getTargetCallNumberRate(snapshot, snapshot.getTotalNumberOfSlowCalls());
  }


  public float getDecimalSuccessRate() {
    Snapshot snapshot = metrics.getSnapshot();
    if (checkNotExceedsMinimumNumberOfCalls(snapshot)) {
      return 0.0f;
    }
    return getTargetCallNumberRate(snapshot, snapshot.getNumberOfSuccessfulCalls());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float getSlowCallRate() {
    return getSlowCallRate(metrics.getSnapshot());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfSuccessfulCalls() {
    return this.metrics.getSnapshot().getNumberOfSuccessfulCalls();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfBufferedCalls() {
    return this.metrics.getSnapshot().getTotalNumberOfCalls();
  }

  @Override
  public int getNumberOfFailedCalls() {
    return this.metrics.getSnapshot().getNumberOfFailedCalls();
  }

  @Override
  public int getNumberOfSlowCalls() {
    return this.metrics.getSnapshot().getTotalNumberOfSlowCalls();
  }

  @Override
  public int getNumberOfSlowSuccessfulCalls() {
    return this.metrics.getSnapshot().getNumberOfSlowSuccessfulCalls();
  }

  @Override
  public int getNumberOfSlowFailedCalls() {
    return this.metrics.getSnapshot().getNumberOfSlowFailedCalls();
  }


  /**
   * {@inheritDoc}
   *
   * Note: the HALF_OPEN state is absent in Probabilistic model.
   */
  @Override
  public long getNumberOfNotPermittedCalls() {
    return this.numberOfNotPermittedCalls.sum();
  }

  enum Result {
    BELOW_THRESHOLDS,
    FAILURE_RATE_ABOVE_THRESHOLDS,
    SLOW_CALL_RATE_ABOVE_THRESHOLDS,
    ABOVE_THRESHOLDS,
    BELOW_MINIMUM_CALLS_THRESHOLD;

    public static boolean hasExceededThresholds(Result result) {
      return hasFailureRateExceededThreshold(result) ||
          hasSlowCallRateExceededThreshold(result);
    }

    public static boolean hasFailureRateExceededThreshold(Result result) {
      return result == ABOVE_THRESHOLDS || result == FAILURE_RATE_ABOVE_THRESHOLDS;
    }

    public static boolean hasSlowCallRateExceededThreshold(Result result) {
      return result == ABOVE_THRESHOLDS || result == SLOW_CALL_RATE_ABOVE_THRESHOLDS;
    }
  }
}
