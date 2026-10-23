package com.dmytrot.agentforge.agent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides the executor on which agent tasks run. */
@Configuration
public class AgentConfig {

  /**
   * Creates a single-thread executor, which also serializes tasks.
   *
   * @return the executor for agent tasks
   */
  @Bean
  public Executor agentExecutor() {
    return Executors.newSingleThreadExecutor();
  }
}
