package com.dmytrot.agentforge.bot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Wires the Telegram client used to send messages. */
@Configuration
public class TelegramConfig {

  /**
   * Creates the client for outgoing calls.
   *
   * @param properties Telegram settings
   * @return a client bound to the bot token
   */
  @Bean
  public TelegramClient telegramClient(BotProperties properties) {
    return new OkHttpTelegramClient(properties.token());
  }
}
