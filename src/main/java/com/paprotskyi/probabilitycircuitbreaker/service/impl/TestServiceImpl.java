package com.paprotskyi.probabilitycircuitbreaker.service.impl;

import com.paprotskyi.probabilitycircuitbreaker.service.TestService;
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
    return null;
  }
}
