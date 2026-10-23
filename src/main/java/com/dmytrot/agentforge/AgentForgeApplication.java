package com.dmytrot.agentforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point of the Telegram bot that drives Claude Code CLI. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AgentForgeApplication {

  /**
   * Starts the Spring application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(AgentForgeApplication.class, args);
  }
}
