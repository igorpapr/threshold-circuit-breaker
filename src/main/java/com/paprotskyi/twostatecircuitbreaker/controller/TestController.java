package com.paprotskyi.twostatecircuitbreaker.controller;

import com.paprotskyi.twostatecircuitbreaker.service.TestService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestController {

  TestService testService;

  @GetMapping("/test-default")
  public boolean testSuccessRate() throws InterruptedException {
    return testService.callExternalServiceWithDefaultBreaker();
  }

  @GetMapping("/test-threshold")
  public boolean testSuccessRateThreshold() throws InterruptedException {
    return testService.callExternalServiceWithThresholdBreaker();
  }
}
