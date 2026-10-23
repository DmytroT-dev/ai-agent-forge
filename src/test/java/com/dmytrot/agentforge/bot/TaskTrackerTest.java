package com.dmytrot.agentforge.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TaskTrackerTest {

  private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

  private final MutableClock clock = new MutableClock(START);
  private final TaskTracker tracker = new TaskTracker(clock);

  @Test
  void allowsOnlyOneTaskAtATime() {
    assertThat(tracker.tryStart()).isTrue();
    assertThat(tracker.tryStart()).isFalse();
  }

  @Test
  void slotIsFreeAgainAfterFinish() {
    tracker.tryStart();

    tracker.finish();

    assertThat(tracker.runningFor()).isEmpty();
    assertThat(tracker.tryStart()).isTrue();
  }

  @Test
  void reportsElapsedTime() {
    tracker.tryStart();

    clock.advance(Duration.ofSeconds(75));

    assertThat(tracker.runningFor()).contains(Duration.ofSeconds(75));
  }

  private static final class MutableClock extends Clock {

    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
