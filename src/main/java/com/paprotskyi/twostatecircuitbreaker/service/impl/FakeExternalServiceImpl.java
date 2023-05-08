package com.paprotskyi.twostatecircuitbreaker.service.impl;

import com.paprotskyi.twostatecircuitbreaker.exception.FailResponseException;
import com.paprotskyi.twostatecircuitbreaker.service.FakeExternalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This service imitates an interaction with an actual external service, which can have one of two states,
 * described below. All the random values are calculated with the same seed each time the service is initialized.
 * The STATE: UP or DOWN
 * if UP:
 * either:
 * success, but with randomly slow or non-slow response
 * fail, with randomly slow or non-slow response
 * if DOWN:
 * fail, so response with max timeout
 */
@Slf4j
@Service
public class FakeExternalServiceImpl implements FakeExternalService {

  private static final long RANDOM_SEED = 125L;
  private static final long CLOCK_RANDOM_SEED = 130L;
  private static final int MIN_RESPONSE_TIME = 50; //50 ms
  private static final int MAX_RESPONSE_TIME = 10000; //10 seconds
  private final Random timeRandom;
  private final Random successFailRandom;
  private final StateTimer stateTimer;

  public FakeExternalServiceImpl() {
    this.timeRandom = new Random(RANDOM_SEED);
    this.successFailRandom = new Random(RANDOM_SEED);
    this.stateTimer = new StateTimer(CLOCK_RANDOM_SEED);
  }

  /**
   * Returns either a successful response or throws a {@link FailResponseException FailResponseException}
   * (meaning failure) depending on the state of the fake service:
   * if it is UP, the random response time is calculated after that delay,
   * either the failure or successful response is returned.
   * if it is DOWN, the failure response is returned after the MAXIMUM response time
   *
   * @return response depending on the state of this service
   */
  @Override
  public boolean generateRandomResponseWithSameSeed() throws InterruptedException {
    boolean isUp = stateTimer.getState();
    if (isUp) {
      int randomTime = MIN_RESPONSE_TIME + timeRandom.nextInt(MAX_RESPONSE_TIME - MIN_RESPONSE_TIME);
      log.info("Locking for time: {} before response", randomTime);
      Thread.sleep(randomTime);
      //TODO probably consider removing and test only on the successful responses,
      // not considering the business exceptions.
      if (successFailRandom.nextBoolean()) {
        return true;
      } else {
        throw new FailResponseException();
      }
    } else {
      Thread.sleep(MAX_RESPONSE_TIME);
      throw new FailResponseException();
    }
  }

  /**
   * This timer changes the state of this service,
   */
  private static class StateTimer {
    private final AtomicBoolean state;
    private final Random random;
    private final Timer timer = new Timer();

    private static final float STATE_CHANGE_PROBABILITY = 0.2f;
    private static final int DELAY_MIN_TIME = 1000; //1 sec
    private static final int DELAY_MAX_TIME = 3000; //3 sec

    public StateTimer(long clockRandomSeed) {
      this.random = new Random(clockRandomSeed);
      this.state = new AtomicBoolean(true);
      scheduleRandomStateChange();
    }

    public boolean getState() {
      return state.get();
    }

    /**
     * Changes the state from UP to DOWN after the random delay with a random probability.
     * After the transition to DOWN, the state is reset to UP after the random delay.
     */
    private void scheduleRandomStateChange() {
      int delay = DELAY_MIN_TIME + random.nextInt(DELAY_MAX_TIME - DELAY_MIN_TIME);
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          if (state.get()) {
            double randomValue = random.nextDouble();
            log.info("Service is UP, generated random value/probability to change: {}/{}",
                randomValue, STATE_CHANGE_PROBABILITY);
            if (randomValue < STATE_CHANGE_PROBABILITY) {
              log.info("Setting fake service state to DOWN");
              state.set(false);
              scheduleRandomStateChange();
            } else {
              log.info("Will not set the state to DOWN at this moment, (rand. value = {})", randomValue);
              scheduleRandomStateChange();
            }
          } else {
            int duration = DELAY_MIN_TIME + random.nextInt(DELAY_MAX_TIME - DELAY_MIN_TIME);
            log.info("Reset the state to UP in: {}", duration);
            timer.schedule(new TimerTask() {
              @Override
              public void run() {
                log.info("Setting fake service state to UP");
                state.set(true);
                scheduleRandomStateChange();
              }
            }, duration);
          }
        }
      }, delay);
    }
  }
}
