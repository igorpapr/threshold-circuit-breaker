package com.paprotskyi.twostatecircuitbreaker.exception;

public class ProbabilityCalculationException extends RuntimeException {

  public ProbabilityCalculationException() {
  }

  public ProbabilityCalculationException(String message) {
    super(message);
  }
}
