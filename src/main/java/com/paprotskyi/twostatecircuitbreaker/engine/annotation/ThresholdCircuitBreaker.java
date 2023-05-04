package com.paprotskyi.twostatecircuitbreaker.engine.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class is a copy of
 * {@link io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker CircuitBreaker annotation},
 * but it is used for the ThresholdCircuitBreaker implementation in this study.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface ThresholdCircuitBreaker {

  String name();

  String fallbackMethod() default "";
}

