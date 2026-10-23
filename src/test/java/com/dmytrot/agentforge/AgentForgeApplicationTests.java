package com.dmytrot.agentforge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// The starter registers the bot on startup and calls the real Telegram API,
// so its auto-configuration is excluded to keep the test offline.
@SpringBootTest(
    properties = {
      "telegram.token=test-token",
      "telegram.allowed-user-id=1",
      "spring.autoconfigure.exclude="
          + "org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration"
    })
class AgentForgeApplicationTests {

  @Test
  void contextLoads() {}
}
