package com.paprotskyi.twostatecircuitbreaker.engine;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.AbstractRegistry;

import java.time.Clock;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

/**
 * This class is the alternative registry for creating and holding ThresholdCircuitBreaker objects.
 */
public class ThresholdCircuitBreakerRegistry extends
    AbstractRegistry<CircuitBreaker, CircuitBreakerConfig> implements CircuitBreakerRegistry {

  /**
   * The constructor with custom default config.
   *
   * @param defaultConfig The default config.
   */
  public ThresholdCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig) {
    super(defaultConfig);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<CircuitBreaker> getAllCircuitBreakers() {
    return new HashSet<>(entryMap.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CircuitBreaker circuitBreaker(String name) {
    return circuitBreaker(name, getDefaultConfig());
  }

  @Override
  public CircuitBreaker circuitBreaker(String name, Map<String, String> tags) {
    return circuitBreaker(name, getDefaultConfig(), tags);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig config) {
    return circuitBreaker(name, config, emptyMap());
  }

  @Override
  public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig config,
                                       Map<String, String> tags) {
    return computeIfAbsent(name, () -> new ThresholdCircuitBreaker(
        name, Clock.systemUTC(), Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CircuitBreaker circuitBreaker(String name, String configName) {
    return circuitBreaker(name, configName, emptyMap());
  }

  @Override
  public CircuitBreaker circuitBreaker(String name, String configName, Map<String, String> tags) {
    throw new RuntimeException("This case is not considered in this study");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CircuitBreaker circuitBreaker(String name,
                                       Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier) {
    throw new RuntimeException("This case is not considered in this study");
  }

  @Override
  public CircuitBreaker circuitBreaker(String name,
                                       Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier,
                                       Map<String, String> tags) {
    throw new RuntimeException("This case is not considered in this study");
  }
}
