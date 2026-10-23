package com.dmytrot.agentforge.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class EchoUpdateConsumerTest {

  private static final long ALLOWED = 42L;

  private final TelegramClient client = mock(TelegramClient.class);
  private final EchoUpdateConsumer consumer =
      new EchoUpdateConsumer(new UserWhitelist(new BotProperties("token", ALLOWED)), client);

  @Test
  void echoesTextFromAllowedUser() throws TelegramApiException {
    consumer.consume(textUpdate(ALLOWED, 100L, "hello"));

    ArgumentCaptor<SendMessage> sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(sent.capture());
    assertThat(sent.getValue().getText()).isEqualTo("hello");
    assertThat(sent.getValue().getChatId()).isEqualTo("100");
  }

  @Test
  void ignoresStranger() throws TelegramApiException {
    consumer.consume(textUpdate(7L, 100L, "hello"));

    verify(client, never()).execute(any(SendMessage.class));
  }

  @Test
  void ignoresUpdateWithoutText() throws TelegramApiException {
    consumer.consume(new Update());

    verify(client, never()).execute(any(SendMessage.class));
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