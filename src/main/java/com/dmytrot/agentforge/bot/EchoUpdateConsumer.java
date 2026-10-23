package com.dmytrot.agentforge.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Replies to the whitelisted user with the same text. A placeholder for the real agent. */
@Component
public class EchoUpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

  private static final Logger log = LoggerFactory.getLogger(EchoUpdateConsumer.class);

  private final UserWhitelist whitelist;
  private final TelegramClient client;

  /**
   * Creates the consumer.
   *
   * @param whitelist access rules
   * @param client client used to send replies
   */
  public EchoUpdateConsumer(UserWhitelist whitelist, TelegramClient client) {
    this.whitelist = whitelist;
    this.client = client;
  }

  @Override
  public void consume(Update update) {
    if (!update.hasMessage() || !update.getMessage().hasText()) {
      return;
    }
    // Strangers get no reply at all, so the bot does not confirm its existence to them.
    if (!whitelist.isAllowed(update.getMessage().getFrom().getId())) {
      log.warn("Ignored a message from a non-whitelisted user");
      return;
    }
    SendMessage reply =
        SendMessage.builder()
            .chatId(update.getMessage().getChatId())
            .text(update.getMessage().getText())
            .build();
    try {
      client.execute(reply);
    } catch (TelegramApiException e) {
      log.error("Failed to send the reply", e);
    }
  }
}