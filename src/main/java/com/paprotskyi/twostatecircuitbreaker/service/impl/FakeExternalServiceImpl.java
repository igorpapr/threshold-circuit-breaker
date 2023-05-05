package com.paprotskyi.twostatecircuitbreaker.service.impl;

import com.paprotskyi.twostatecircuitbreaker.service.FakeExternalService;

public class FakeExternalServiceImpl implements FakeExternalService {

  private static final long RANDOM_SEED = 125L;

  @Override
  public String generateRandomResponseWithSameSeed(String param) {
    return null;
  }

  //TODO compare number of successful responses using the same random seed under high load
}
