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

//  public ThresholdCircuitBreakerRegistry() {
//    this(emptyMap());
//  }

//  public ThresholdCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs) {
//    this(configs, emptyMap());
//  }
//
//  public ThresholdCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs,
//                                         Map<String, String> tags) {
//    this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()), tags);
//    this.configurations.putAll(configs);
//  }

//  public ThresholdCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs,
//                                        RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
//    this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()),
//        registryEventConsumer);
//    this.configurations.putAll(configs);
//  }

//  public ThresholdCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs,
//                                        RegistryEventConsumer<CircuitBreaker> registryEventConsumer, Map<String, String> tags) {
//    this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()),
//        registryEventConsumer, tags);
//    this.configurations.putAll(configs);
//  }

//  public ThresholdCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs,
//                                        List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers,
//                                        Map<String, String> tags, RegistryStore<CircuitBreaker> registryStore) {
//    super(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()),
//        registryEventConsumers, Optional.ofNullable(tags).orElse(emptyMap()),
//        Optional.ofNullable(registryStore).orElse(new InMemoryRegistryStore<>()));
//    this.configurations.putAll(configs);
//  }

//  public ThresholdCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs,
//                                        List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers) {
//    this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()),
//        registryEventConsumers);
//    this.configurations.putAll(configs);
//  }

  /**
   * The constructor with custom default config.
   *
   * @param defaultConfig The default config.
   */
  public ThresholdCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig) {
    super(defaultConfig);
  }

  //TODO remove these commented if event consumers will not be needed
//  /**
//   * The constructor with custom default config.
//   *
//   * @param defaultConfig The default config.
//   * @param tags          The tags to add to the CircuitBreaker
//   */
//  public ThresholdCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig,
//                                        Map<String, String> tags) {
//    super(defaultConfig, tags);
//  }
//
//  public ThresholdCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig,
//                                        RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
//    super(defaultConfig, registryEventConsumer);
//  }
//
//  public ThresholdCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig,
//                                        RegistryEventConsumer<CircuitBreaker> registryEventConsumer,
//                                        Map<String, String> tags) {
//    super(defaultConfig, registryEventConsumer, tags);
//  }
//
//  public ThresholdCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig,
//                                        List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers) {
//    super(defaultConfig, registryEventConsumers);
//  }

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
                                       Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier, Map<String, String> tags) {
    throw new RuntimeException("This case is not considered in this study");
  }
}
