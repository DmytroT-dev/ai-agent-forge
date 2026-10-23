package com.dmytrot.agentforge.bot;

import org.springframework.stereotype.Component;

/** Decides who may talk to the bot. Kept free of Telegram types so it is trivial to test. */
@Component
public class UserWhitelist {

  private final long allowedUserId;

  /**
   * Creates a whitelist with the single configured user.
   *
   * @param properties Telegram settings
   */
  public UserWhitelist(BotProperties properties) {
    this.allowedUserId = properties.allowedUserId();
  }

  /**
   * Checks a sender.
   *
   * @param userId Telegram user id of the sender
   * @return {@code true} only for the configured user
   */
  public boolean isAllowed(long userId) {
    return userId == allowedUserId;
  }
}