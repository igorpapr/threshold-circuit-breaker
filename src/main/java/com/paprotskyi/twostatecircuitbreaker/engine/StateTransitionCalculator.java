package com.paprotskyi.twostatecircuitbreaker.engine;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StateTransitionCalculator {

  private static final long DEFAULT_OPEN_STATE_DURATION_THRESHOLD = 10_000_000_000L;

  private static final float SLOW_CALL_RATE_COEFFICIENT = 0.15f;
  private static final float FAILURE_RATE_COEFFICIENT = 0.4f;
  private static final float SUCCESS_CALL_RATE_COEFFICIENT = 0.35f;
  private static final float TIME_IN_OPEN_STATE_COEFFICIENT = 0.1f;

  public float calculateTransitionValue(@NonNull SimpleMetrics metrics,
                                        long currentOpenStateDurationInNanos) {
    //always close the circuit breaker when the time in open state is longer than the given threshold
    if (currentOpenStateDurationInNanos > DEFAULT_OPEN_STATE_DURATION_THRESHOLD) {
      return Float.POSITIVE_INFINITY;
    }

    float decimalFailureRating = (1 - metrics.getDecimalFailureRate()) * FAILURE_RATE_COEFFICIENT;
    float decimalSlowCallRating = (1 - metrics.getDecimalSlowCallRate()) * SLOW_CALL_RATE_COEFFICIENT;
    float decimalSuccessCallRating = metrics.getDecimalSuccessRate() * SUCCESS_CALL_RATE_COEFFICIENT;

    //time in open state
    float timeInOpenStateRating = (float)
        currentOpenStateDurationInNanos / DEFAULT_OPEN_STATE_DURATION_THRESHOLD * TIME_IN_OPEN_STATE_COEFFICIENT;

    return decimalFailureRating + decimalSlowCallRating + decimalSuccessCallRating + timeInOpenStateRating;
  }
}
