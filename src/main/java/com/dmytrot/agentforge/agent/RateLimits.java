package com.dmytrot.agentforge.agent;

import java.time.Instant;

/**
 * Subscription usage limits reported by the CLI.
 *
 * @param fiveHour the rolling five-hour window, {@code null} if not reported
 * @param sevenDay the weekly window, {@code null} if not reported
 */
public record RateLimits(Window fiveHour, Window sevenDay) {

  /**
   * One limit window.
   *
   * @param utilization used share of the window, from 0.0 to 1.0
   * @param resetsAt when the window starts over
   */
  public record Window(double utilization, Instant resetsAt) {

    /**
     * Returns the used share as a percentage.
     *
     * @return whole percent of the window already used
     */
    public int percentUsed() {
      return (int) Math.round(utilization * 100);
    }
  }
}
