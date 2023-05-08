package com.paprotskyi.twostatecircuitbreaker.service;

public interface FakeExternalService {

  boolean generateRandomResponseWithSameSeed() throws InterruptedException;

}
