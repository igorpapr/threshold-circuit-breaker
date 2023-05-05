package com.paprotskyi.twostatecircuitbreaker.service;

public interface TestService {

  String callExternalServiceWithDefaultBreaker(String parameter1);

  String callExternalServiceWithThresholdBreaker(String parameter1);

}
