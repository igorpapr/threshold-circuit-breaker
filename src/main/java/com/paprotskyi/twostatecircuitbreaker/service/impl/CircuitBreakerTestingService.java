package com.paprotskyi.twostatecircuitbreaker.service.impl;

import com.paprotskyi.twostatecircuitbreaker.engine.ThresholdCircuitBreakerRegistry;
import com.paprotskyi.twostatecircuitbreaker.engine.annotation.ThresholdCircuitBreaker;
import com.paprotskyi.twostatecircuitbreaker.service.TestService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CircuitBreakerTestingService implements TestService {

  ThresholdCircuitBreakerRegistry thresholdCircuitBreakerRegistry;
  CircuitBreakerRegistry defaultCircuitBreakerRegistry;
  FakeExternalServiceImpl fakeExternalService;

  public CircuitBreakerTestingService(
      @Qualifier("thresholdCircuitBreakerRegistry") ThresholdCircuitBreakerRegistry thresholdCircuitBreakerRegistry,
      @Qualifier("circuitBreakerRegistry") CircuitBreakerRegistry defaultCircuitBreakerRegistry,
      FakeExternalServiceImpl fakeExternalService) {
    this.thresholdCircuitBreakerRegistry = thresholdCircuitBreakerRegistry;
    this.defaultCircuitBreakerRegistry = defaultCircuitBreakerRegistry;
    this.fakeExternalService = fakeExternalService;
  }

  @Override
  @CircuitBreaker(name = "service_default_breaker", fallbackMethod = "fallback")
  public boolean callExternalServiceWithDefaultBreaker() throws InterruptedException {
    return fakeExternalService.generateRandomResponseWithSameSeed();
  }

  @Override
  @ThresholdCircuitBreaker(name = "service_threshold_breaker", fallbackMethod = "fallback")
  public boolean callExternalServiceWithThresholdBreaker() throws InterruptedException {
    return fakeExternalService.generateRandomResponseWithSameSeed();
  }

  private boolean fallback(Exception e) {
//    com.paprotskyi.twostatecircuitbreaker.engine.ThresholdCircuitBreaker thresholdCircuitBreaker =
//        (com.paprotskyi.twostatecircuitbreaker.engine.ThresholdCircuitBreaker)
//            thresholdCircuitBreakerRegistry.getAllCircuitBreakers().iterator().next();
//    Metrics metrics = thresholdCircuitBreaker.getMetrics();
    log.info("Recovered fallback");//, metrics);
    return false;
  }

}
