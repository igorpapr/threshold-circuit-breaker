package com.paprotskyi.twostatecircuitbreaker.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestController {

  public String sampleEndpoint() {
    return null;
  }

  /*
  TODO:
  - try configuring the circuit breaker using resilience4j
  - find its inner "guts" - what's inside them? Check documentation whether there's something interesting
  - try writing the engine, decide between 2 variants:
    - overriding the resilience4j inner logic if it is possible
    - I saw some methods to change the state manually - check maybe it would be useful here
    - try writing my own version of circuit breaker
    - MANDATORY: write own probability engine using metrics to use from the circuit breaker
   */

}
