package com.paprotskyi.twostatecircuitbreaker.exception;

public class IncorrectStateLogicException extends RuntimeException {

  public IncorrectStateLogicException() {
    super();
  }

  public IncorrectStateLogicException(String message) {
    super(message);
  }

  public IncorrectStateLogicException(String message, Throwable cause) {
    super(message, cause);
  }
}
