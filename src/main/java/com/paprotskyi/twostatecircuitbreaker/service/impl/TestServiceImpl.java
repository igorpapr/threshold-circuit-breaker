package com.paprotskyi.twostatecircuitbreaker.service.impl;

import com.paprotskyi.twostatecircuitbreaker.engine.ThresholdCircuitBreakerRegistry;
import com.paprotskyi.twostatecircuitbreaker.engine.annotation.ThresholdCircuitBreaker;
import com.paprotskyi.twostatecircuitbreaker.service.TestService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

  ThresholdCircuitBreakerRegistry thresholdCircuitBreakerRegistry;

  CircuitBreakerRegistry defaultCircuitBreakerRegistry;


  @Override
  @CircuitBreaker(name = "service_default_breaker", fallbackMethod = "fallback")
  public String callExternalServiceWithDefaultBreaker(String parameter1) {
    return null;
  }

  @Override
  @ThresholdCircuitBreaker(name = "service_threshold_breaker", fallbackMethod = "fallback")
  public String callExternalServiceWithThresholdBreaker(String parameter1) {
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
    com.paprotskyi.twostatecircuitbreaker.engine.ThresholdCircuitBreaker thresholdCircuitBreaker =
        (com.paprotskyi.twostatecircuitbreaker.engine.ThresholdCircuitBreaker)
            thresholdCircuitBreakerRegistry.getAllCircuitBreakers().iterator().next();
    Metrics metrics = thresholdCircuitBreaker.getMetrics();
    log.info("Metrics: {}", metrics);
    return "Recovered: " + ex.toString();
  }
}
