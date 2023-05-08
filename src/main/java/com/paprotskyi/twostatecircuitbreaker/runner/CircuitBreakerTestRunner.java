package com.paprotskyi.twostatecircuitbreaker.runner;

import com.paprotskyi.twostatecircuitbreaker.service.TestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CircuitBreakerTestRunner implements ApplicationRunner {

  private final int numberOfCalls;
  private final TestService testService;

  public CircuitBreakerTestRunner(@Value("${number-of-test-calls}") int numberOfCalls,
                                  TestService testService) {
    this.numberOfCalls = numberOfCalls;
    this.testService = testService;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    int counter = 0;
    for (int i = 0; i < numberOfCalls; i++) {
      log.info("making request: {}", i);
      if (testService.callExternalServiceWithDefaultBreaker()) {
      //if (testService.callExternalServiceWithThresholdBreaker()) {
        counter++;
      }
      Thread.sleep(500);
    }
    float result = (float) counter / numberOfCalls;
    log.error("RESULT RATE: {}", result);
  }
}
