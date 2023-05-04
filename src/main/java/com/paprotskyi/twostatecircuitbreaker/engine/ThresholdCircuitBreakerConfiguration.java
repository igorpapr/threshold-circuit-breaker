package com.paprotskyi.twostatecircuitbreaker.engine;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
@EnableConfigurationProperties(CircuitBreakerProperties.class)
public class ThresholdCircuitBreakerConfiguration {

  //Has protected access in the original library functionality
  private static final String DEFAULT_CONFIG_KEY = "default";

  @Bean
  @Qualifier("thresholdRegistryConfig")
  public CircuitBreakerConfig thresholdRegistryConfig(@Autowired CircuitBreakerProperties circuitBreakerProperties) {
    CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultInstanceProperties =
        circuitBreakerProperties.getConfigs().get(DEFAULT_CONFIG_KEY);
    if (defaultInstanceProperties != null) {
      CompositeCustomizer<CircuitBreakerConfigCustomizer> emptyCustomizer =
          new CompositeCustomizer<>(Collections.emptyList());
      return circuitBreakerProperties
          .createCircuitBreakerConfig(DEFAULT_CONFIG_KEY, defaultInstanceProperties, emptyCustomizer);
    }
    return CircuitBreakerConfig.ofDefaults();
  }

  @Bean
  public ThresholdCircuitBreakerRegistry thresholdCircuitBreakerRegistry(
      @Qualifier("thresholdRegistryConfig") CircuitBreakerConfig config) {
    return new ThresholdCircuitBreakerRegistry(config);
  }

}
