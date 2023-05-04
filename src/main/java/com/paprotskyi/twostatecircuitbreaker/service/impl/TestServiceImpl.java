package com.paprotskyi.twostatecircuitbreaker.service.impl;

import com.paprotskyi.twostatecircuitbreaker.engine.ThresholdCircuitBreakerRegistry;
import com.paprotskyi.twostatecircuitbreaker.engine.annotation.ThresholdCircuitBreaker;
import com.paprotskyi.twostatecircuitbreaker.service.TestService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestServiceImpl implements TestService {

  ThresholdCircuitBreakerRegistry registry;

  @Override
  @ThresholdCircuitBreaker(name = "service_threshold_a", fallbackMethod = "fallback")
  public String callExternalService(String parameter1) {
    float failureProbability = 0.5f;
    float value = ThreadLocalRandom.current().nextFloat(0, 1.0f);
    log.error("Value: {}", value);
    if (value < failureProbability) {
      log.error("failure probability worked");
      throw new RuntimeException("Test");
    }
    return "Success";
  }

  private String fallback(Exception ex) {
    com.paprotskyi.twostatecircuitbreaker.engine.ThresholdCircuitBreaker
        thresholdCircuitBreaker =
        (com.paprotskyi.twostatecircuitbreaker.engine.ThresholdCircuitBreaker) registry.getAllCircuitBreakers().iterator().next();
    CircuitBreaker.Metrics metrics = thresholdCircuitBreaker.getMetrics();
    log.info("Metrics: {}", metrics);
    return "Recovered: " + ex.toString();
  }
}
