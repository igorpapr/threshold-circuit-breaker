package com.paprotskyi.twostatecircuitbreaker.exception;

public class FailResponseException extends RuntimeException {

  public FailResponseException() {
  }

  public FailResponseException(String message) {
    super(message);
  }
}
