package com.dmytrot.agentforge.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;

/** Registers the update consumer with the long polling starter. */
@Component
public class TelegramBot implements SpringLongPollingBot {

  private final BotProperties properties;
  private final EchoUpdateConsumer consumer;

  /**
   * Creates the bot.
   *
   * @param properties Telegram settings
   * @param consumer handler for incoming updates
   */
  public TelegramBot(BotProperties properties, EchoUpdateConsumer consumer) {
    this.properties = properties;
    this.consumer = consumer;
  }

  @Override
  public String getBotToken() {
    return properties.token();
  }

  @Override
  public LongPollingUpdateConsumer getUpdatesConsumer() {
    return consumer;
  }
}