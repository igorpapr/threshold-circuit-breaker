package com.paprotskyi.twostatecircuitbreaker.service;

public interface FakeExternalService {

  String generateRandomResponseWithSameSeed(String param);

}
