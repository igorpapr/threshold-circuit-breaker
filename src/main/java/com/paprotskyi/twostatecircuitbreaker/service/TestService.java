package com.paprotskyi.twostatecircuitbreaker.service;

public interface TestService {
  boolean callExternalServiceWithDefaultBreaker() throws InterruptedException;

  boolean callExternalServiceWithThresholdBreaker() throws InterruptedException;

}
