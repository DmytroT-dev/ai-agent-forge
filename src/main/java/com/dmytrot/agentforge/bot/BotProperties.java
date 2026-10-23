package com.dmytrot.agentforge.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Telegram settings. Both values come from environment variables, never from the repository.
 *
 * @param token bot token issued by BotFather
 * @param allowedUserId the only Telegram user the bot talks to
 */
@ConfigurationProperties(prefix = "telegram")
public record BotProperties(String token, long allowedUserId) {

  /** Fails fast at startup so a misconfigured bot never runs half-open. */
  public BotProperties {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("telegram.token must be set");
    }
    if (allowedUserId <= 0) {
      throw new IllegalArgumentException("telegram.allowed-user-id must be set");
    }
  }
}