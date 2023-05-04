package com.paprotskyi.twostatecircuitbreaker.engine;

import com.paprotskyi.twostatecircuitbreaker.engine.annotation.ThresholdCircuitBreaker;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerAspectExt;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.utils.AnnotationExtractor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * This class is a copy of {@link CircuitBreakerAspect CircuitBreakerAspect},
 * but it uses the ThresholdCircuitBreakerRegistry for the study.
 */
@Aspect
@Configuration
public class ThresholdCircuitBreakerAspect {

  private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspect.class);

  private final ThresholdCircuitBreakerRegistry thresholdCircuitBreakerRegistry;
  private final @Nullable
  List<CircuitBreakerAspectExt> circuitBreakerAspectExtList;
  private final FallbackExecutor fallbackExecutor;
  private final SpelResolver spelResolver;

  public ThresholdCircuitBreakerAspect(ThresholdCircuitBreakerRegistry thresholdCircuitBreakerRegistry,
                                       @Autowired(required = false)
                                       List<CircuitBreakerAspectExt> circuitBreakerAspectExtList,
                                       FallbackExecutor fallbackExecutor,
                                       SpelResolver spelResolver) {
    this.thresholdCircuitBreakerRegistry = thresholdCircuitBreakerRegistry;
    this.circuitBreakerAspectExtList = circuitBreakerAspectExtList;
    this.fallbackExecutor = fallbackExecutor;
    this.spelResolver = spelResolver;
  }

  @Pointcut(value = "@within(thresholdCircuitBreaker) || @annotation(thresholdCircuitBreaker)",
      argNames = "thresholdCircuitBreaker")
  public void matchAnnotatedClassOrMethod(ThresholdCircuitBreaker thresholdCircuitBreaker) {
  }

  @Around(value = "matchAnnotatedClassOrMethod(circuitBreakerAnnotation)", argNames = "proceedingJoinPoint, circuitBreakerAnnotation")
  public Object thresholdCircuitBreakerAroundAdvice(ProceedingJoinPoint proceedingJoinPoint,
                                                    @Nullable ThresholdCircuitBreaker circuitBreakerAnnotation)
      throws Throwable {
    Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
    String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
    if (circuitBreakerAnnotation == null) {
      circuitBreakerAnnotation = getCircuitBreakerAnnotation(proceedingJoinPoint);
    }
    if (circuitBreakerAnnotation == null) { //because annotations wasn't found
      return proceedingJoinPoint.proceed();
    }
    String backend = spelResolver.resolve(method, proceedingJoinPoint.getArgs(), circuitBreakerAnnotation.name());
    io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(
        methodName, backend);
    Class<?> returnType = method.getReturnType();
    final CheckedSupplier<Object>
        circuitBreakerExecution = () -> proceed(proceedingJoinPoint, methodName, circuitBreaker, returnType);
    return fallbackExecutor.execute(proceedingJoinPoint, method, circuitBreakerAnnotation.fallbackMethod(),
        circuitBreakerExecution);
  }

  private Object proceed(ProceedingJoinPoint proceedingJoinPoint, String methodName,
                         io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker, Class<?> returnType)
      throws Throwable {
    if (circuitBreakerAspectExtList != null && !circuitBreakerAspectExtList.isEmpty()) {
      for (CircuitBreakerAspectExt circuitBreakerAspectExt : circuitBreakerAspectExtList) {
        if (circuitBreakerAspectExt.canHandleReturnType(returnType)) {
          return circuitBreakerAspectExt
              .handle(proceedingJoinPoint, circuitBreaker, methodName);
        }
      }
    }
    if (CompletionStage.class.isAssignableFrom(returnType)) {
      return handleJoinPointCompletableFuture(proceedingJoinPoint, circuitBreaker);
    }
    return defaultHandling(proceedingJoinPoint, circuitBreaker);
  }

  private io.github.resilience4j.circuitbreaker.CircuitBreaker getOrCreateCircuitBreaker(
      String methodName, String backend) {
    io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = thresholdCircuitBreakerRegistry
        .circuitBreaker(backend);

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Created or retrieved circuit breaker '{}' with failure rate '{}' for method: '{}'",
          backend, circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(),
          methodName);
    }

    return circuitBreaker;
  }

  @Nullable
  private ThresholdCircuitBreaker getCircuitBreakerAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
    if (logger.isDebugEnabled()) {
      logger.debug("circuitBreaker parameter is null");
    }
    if (proceedingJoinPoint.getTarget() instanceof Proxy) {
      logger.debug(
          "The threshold circuit breaker annotation is kept on a interface which is acting as a proxy");
      return AnnotationExtractor
          .extractAnnotationFromProxy(proceedingJoinPoint.getTarget(), ThresholdCircuitBreaker.class);
    } else {
      return AnnotationExtractor
          .extract(proceedingJoinPoint.getTarget().getClass(), ThresholdCircuitBreaker.class);
    }
  }

  /**
   * handle the CompletionStage return types AOP based into configured circuit-breaker
   */
  private Object handleJoinPointCompletableFuture(ProceedingJoinPoint proceedingJoinPoint,
                                                  io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker) {
    return circuitBreaker.executeCompletionStage(() -> {
      try {
        return (CompletionStage<?>) proceedingJoinPoint.proceed();
      } catch (Throwable throwable) {
        throw new CompletionException(throwable);
      }
    });
  }

  /**
   * the default Java types handling for the circuit breaker AOP
   */
  private Object defaultHandling(ProceedingJoinPoint proceedingJoinPoint,
                                 io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker) throws Throwable {
    return circuitBreaker.executeCheckedSupplier(proceedingJoinPoint::proceed);
  }

}
