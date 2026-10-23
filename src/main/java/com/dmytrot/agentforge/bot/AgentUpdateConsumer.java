package com.dmytrot.agentforge.bot;

import com.dmytrot.agentforge.agent.AgentException;
import com.dmytrot.agentforge.agent.AgentReply;
import com.dmytrot.agentforge.agent.AgentService;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Forwards messages of the whitelisted user to the agent and sends the answer back. */
@Component
public class AgentUpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

  private static final Logger log = LoggerFactory.getLogger(AgentUpdateConsumer.class);

  private final UserWhitelist whitelist;
  private final AgentService agent;
  private final TelegramClient client;
  private final Executor executor;
  private final AtomicBoolean busy = new AtomicBoolean();

  /**
   * Creates the consumer.
   *
   * @param whitelist access rules
   * @param agent runs tasks on the coding agent
   * @param client client used to send replies
   * @param executor runs tasks off the polling thread, so a long task does not stall updates
   */
  public AgentUpdateConsumer(
      UserWhitelist whitelist,
      AgentService agent,
      TelegramClient client,
      @Qualifier("agentExecutor") Executor executor) {
    this.whitelist = whitelist;
    this.agent = agent;
    this.client = client;
    this.executor = executor;
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
    long chatId = update.getMessage().getChatId();
    // One task at a time: they share one session and one workspace.
    if (!busy.compareAndSet(false, true)) {
      send(chatId, "Still working on the previous task.");
      return;
    }
    String prompt = update.getMessage().getText();
    executor.execute(
        () -> {
          try {
            handle(chatId, prompt);
          } finally {
            busy.set(false);
          }
        });
  }

  private void handle(long chatId, String prompt) {
    try {
      AgentReply reply = agent.ask(prompt);
      String text = reply.text().isBlank() ? "(empty answer)" : reply.text();
      send(chatId, reply.error() ? "Agent error: " + text : text);
    } catch (AgentException e) {
      log.error("Agent run failed", e);
      send(chatId, "The agent failed: " + e.getMessage());
    }
  }

  private void send(long chatId, String text) {
    for (String part : MessageSplitter.split(text, MessageSplitter.TELEGRAM_LIMIT)) {
      try {
        client.execute(SendMessage.builder().chatId(chatId).text(part).build());
      } catch (TelegramApiException e) {
        log.error("Failed to send a message", e);
        return;
      }
    }
  }
}
