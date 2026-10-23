package com.dmytrot.agentforge.bot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/** Knows whether a task is running and since when. Only one task runs at a time. */
@Component
public class TaskTracker {

  private final Clock clock;
  private final AtomicReference<Instant> startedAt = new AtomicReference<>();

  /**
   * Creates a tracker.
   *
   * @param clock time source
   */
  public TaskTracker(Clock clock) {
    this.clock = clock;
  }

  /**
   * Claims the single task slot.
   *
   * @return {@code true} if the caller may start a task, {@code false} if one is already running
   */
  public boolean tryStart() {
    return startedAt.compareAndSet(null, clock.instant());
  }

  /** Releases the task slot. */
  public void finish() {
    startedAt.set(null);
  }

  /**
   * Tells how long the current task has been running.
   *
   * @return the elapsed time, or empty if nothing is running
   */
  public Optional<Duration> runningFor() {
    Instant start = startedAt.get();
    return start == null ? Optional.empty() : Optional.of(Duration.between(start, clock.instant()));
  }
}
