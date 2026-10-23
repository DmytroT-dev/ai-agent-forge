package com.dmytrot.agentforge.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContextUsageTest {

  @Test
  void computesRemainingAndPercent() {
    ContextUsage usage = new ContextUsage(250, 1_000);

    assertThat(usage.remainingTokens()).isEqualTo(750);
    assertThat(usage.percentLeft()).isEqualTo(75);
  }

  @Test
  void remainingNeverGoesNegative() {
    assertThat(new ContextUsage(2_000, 1_000).remainingTokens()).isZero();
  }
}
