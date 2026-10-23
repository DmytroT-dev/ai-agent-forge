package com.dmytrot.agentforge.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmytrot.agentforge.agent.AgentCancelledException;
import com.dmytrot.agentforge.agent.AgentException;
import com.dmytrot.agentforge.agent.AgentReply;
import com.dmytrot.agentforge.agent.AgentService;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class AgentUpdateConsumerTest {

  private static final long ALLOWED = 42L;

  private final AgentService agent = mock(AgentService.class);
  private final TelegramClient client = mock(TelegramClient.class);
  private final BotCommands commands = mock(BotCommands.class);
  private final TaskTracker tracker = new TaskTracker(Clock.systemUTC());
  // Runs tasks inline so the tests need no waiting.
  private final AgentUpdateConsumer consumer =
      new AgentUpdateConsumer(
          new UserWhitelist(new BotProperties("token", ALLOWED)),
          agent,
          commands,
          tracker,
          client,
          Runnable::run);

  @Test
  void sendsAgentAnswerToAllowedUser() throws TelegramApiException {
    when(agent.ask("hello")).thenReturn(new AgentReply("answer", "s1", false, null, null));

    consumer.consume(textUpdate(ALLOWED, 100L, "hello"));

    assertThat(sentTexts()).containsExactly("answer");
  }

  @Test
  void reportsAgentFailure() throws TelegramApiException {
    when(agent.ask("hello")).thenThrow(new AgentException("timeout"));

    consumer.consume(textUpdate(ALLOWED, 100L, "hello"));

    assertThat(sentTexts()).containsExactly("The agent failed: timeout");
  }

  @Test
  void routesCommandsToHandlerNotToAgent() throws TelegramApiException {
    when(commands.handle("/status")).thenReturn("status text");

    consumer.consume(textUpdate(ALLOWED, 100L, "/status"));

    assertThat(sentTexts()).containsExactly("status text");
    verify(agent, never()).ask(any());
  }

  @Test
  void continueCommandRunsFixedPromptThroughAgent() throws TelegramApiException {
    when(agent.ask("Continue the task you were working on before it was interrupted."))
        .thenReturn(new AgentReply("resumed", "s1", false, null, null));

    consumer.consume(textUpdate(ALLOWED, 100L, "/continue"));

    assertThat(sentTexts()).containsExactly("resumed");
    verify(commands, never()).handle(any());
  }

  @Test
  void reportsStoppedTask()throws TelegramApiException {
    when(agent.ask("hello")).thenThrow(new AgentCancelledException(null));

    consumer.consume(textUpdate(ALLOWED, 100L, "hello"));

    assertThat(sentTexts()).containsExactly("The task was stopped.");
  }

  @Test
  void rejectsSecondTaskWhileOneRuns() throws TelegramApiException {
    tracker.tryStart();

    consumer.consume(textUpdate(ALLOWED, 100L, "hello"));

    verify(agent, never()).ask(any());
    assertThat(sentTexts()).singleElement().asString().contains("Still working");
  }

  @Test
  void ignoresStrangerWithoutCallingAgent() throws TelegramApiException {
    consumer.consume(textUpdate(7L, 100L, "hello"));

    verify(agent, never()).ask(any());
    verify(client, never()).execute(any(SendMessage.class));
  }

  @Test
  void ignoresUpdateWithoutText() throws TelegramApiException {
    consumer.consume(new Update());

    verify(agent, never()).ask(any());
  }

  private List<String> sentTexts() throws TelegramApiException {
    ArgumentCaptor<SendMessage> sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client, atLeastOnce()).execute(sent.capture());
    return sent.getAllValues().stream().map(SendMessage::getText).toList();
  }

  private static Update textUpdate(long userId, long chatId, String text) {
    Message message = new Message();
    message.setFrom(new User(userId, "name", false));
    message.setChat(new Chat(chatId, "private"));
    message.setText(text);
    Update update = new Update();
    update.setMessage(message);
    return update;
  }
}
