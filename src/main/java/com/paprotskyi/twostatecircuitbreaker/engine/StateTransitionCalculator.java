package com.paprotskyi.twostatecircuitbreaker.engine;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StateTransitionCalculator {

  private static final long DEFAULT_OPEN_STATE_DURATION_THRESHOLD = 10_000_000_000L;

  private static final float SLOW_CALL_RATE_COEFFICIENT = 0.25f;
  private static final float FAILURE_RATE_COEFFICIENT = 0.15f;
  private static final float SUCCESS_CALL_RATE_COEFFICIENT = 0.1f;
  private static final float TIME_IN_OPEN_STATE_COEFFICIENT = 0.5f;
  private static final float AVERAGE_DURATION_OF_CALLS_COEFFICIENT = 0.1f;
  private static final int DEFAULT_NUMBER_OF_BUCKETS_FOR_DURATION = 3;


  //todo probably add check for configuration that all the coefficients sum up as 1
  public static float calculateTransitionValue(@NonNull SimpleMetrics metrics,
                                               long currentOpenStateDurationInNanos) {
    //always close the circuit breaker when the time in open state is longer than the given threshold
    if (currentOpenStateDurationInNanos > DEFAULT_OPEN_STATE_DURATION_THRESHOLD) {
      return Float.POSITIVE_INFINITY;
    }

    //todo: should we use this one, or the original one, which works with minimumNumberOfCalls variable
    float decimalFailureRating = (1 - metrics.getDecimalFailureRate()) * FAILURE_RATE_COEFFICIENT;
    float decimalSlowCallRating = (1 - metrics.getDecimalSlowCallRate()) * SLOW_CALL_RATE_COEFFICIENT;
    float decimalSuccessCallRating = metrics.getDecimalSuccessRate() * SUCCESS_CALL_RATE_COEFFICIENT;

    //time in open state
    float timeInOpenStateRating = (float)
        currentOpenStateDurationInNanos / DEFAULT_OPEN_STATE_DURATION_THRESHOLD * TIME_IN_OPEN_STATE_COEFFICIENT;
    //TODO try getting this information later
    //avg duration of calls in last N (maybe just one) buckets / avg duration in all buckets - bigger than one or lower

    return decimalFailureRating + decimalSlowCallRating + decimalSuccessCallRating + timeInOpenStateRating;
  }


}
