package com.paprotskyi.twostatecircuitbreaker.service.impl;

import com.paprotskyi.twostatecircuitbreaker.service.TestService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestServiceImpl implements TestService {

  @Override
  public String callExternalService(String parameter1) {

    //todo find how to create my circuit breaker using circuit breaker registry, maybe add custom impl
    return null;
  }
}
