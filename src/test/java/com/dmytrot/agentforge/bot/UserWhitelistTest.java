package com.dmytrot.agentforge.bot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserWhitelistTest {

  private final UserWhitelist whitelist = new UserWhitelist(new BotProperties("token", 42L));

  @Test
  void allowsConfiguredUser() {
    assertThat(whitelist.isAllowed(42L)).isTrue();
  }

  @Test
  void rejectsAnyoneElse() {
    assertThat(whitelist.isAllowed(7L)).isFalse();
  }
}