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
  private final String testMode;

  public CircuitBreakerTestRunner(@Value("${number-of-test-calls}") int numberOfCalls,
                                  @Value("${test-mode}") String testMode,
                                  TestService testService) {
    this.numberOfCalls = numberOfCalls;
    this.testService = testService;
    this.testMode = testMode;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    int counter = 0;
    for (int i = 0; i < numberOfCalls; i++) {
      log.info("making request: {}", i);
      if (makeTargetCallGetBooleanResponse()) {
        counter++;
      }
      Thread.sleep(500);
    }
    float result = (float) counter / numberOfCalls;
    log.info("\n\n======================RESULT RATE: {}=======================\n\n", result);
  }

  private boolean makeTargetCallGetBooleanResponse() throws InterruptedException {
    return testMode.equals("success_rate_traditional")
        ? testService.callExternalServiceWithDefaultBreaker()
        : testService.callExternalServiceWithThresholdBreaker();
  }
}
