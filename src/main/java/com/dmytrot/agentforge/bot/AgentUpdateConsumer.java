package com.dmytrot.agentforge.bot;

import com.dmytrot.agentforge.agent.AgentCancelledException;
import com.dmytrot.agentforge.agent.AgentException;
import com.dmytrot.agentforge.agent.AgentReply;
import com.dmytrot.agentforge.agent.AgentService;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Routes messages of the whitelisted user: commands to {@link BotCommands}, text to the agent. */
@Component
public class AgentUpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

  private static final Logger log = LoggerFactory.getLogger(AgentUpdateConsumer.class);

  private final UserWhitelist whitelist;
  private final AgentService agent;
  private final BotCommands commands;
  private final TaskTracker tracker;
  private final TelegramClient client;
  private final Executor executor;

  /**
   * Creates the consumer.
   *
   * @param whitelist access rules
   * @param agent runs tasks on the coding agent
   * @param commands handles slash commands
   * @param tracker allows one task at a time
   * @param client client used to send replies
   * @param executor runs tasks off the polling thread, so a long task does not stall updates
   */
  public AgentUpdateConsumer(
      UserWhitelist whitelist,
      AgentService agent,
      BotCommands commands,
      TaskTracker tracker,
      TelegramClient client,
      @Qualifier("agentExecutor") Executor executor) {
    this.whitelist = whitelist;
    this.agent = agent;
    this.commands = commands;
    this.tracker = tracker;
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
    String text = update.getMessage().getText();
    if (text.startsWith("/")) {
      send(chatId, commands.handle(text));
      return;
    }
    // One task at a time: they share one session and one workspace.
    if (!tracker.tryStart()) {
      send(chatId, "Still working on the previous task. Send /stop to cancel it.");
      return;
    }
    executor.execute(
        () -> {
          try {
            handle(chatId, text);
          } finally {
            tracker.finish();
          }
        });
  }

  private void handle(long chatId, String prompt) {
    try {
      AgentReply reply = agent.ask(prompt);
      String text = reply.text().isBlank() ? "(empty answer)" : reply.text();
      send(chatId, reply.error() ? "Agent error: " + text : text);
    } catch (AgentCancelledException e) {
      send(chatId, "The task was stopped.");
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
