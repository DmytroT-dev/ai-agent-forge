package com.dmytrot.agentforge.agent;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for launching Claude Code CLI.
 *
 * @param command executable name or path, for example {@code claude} or {@code claude.cmd}
 * @param workspace directory the agent works in; must be outside this repository
 * @param timeout how long one task may run before the process is killed
 * @param permissionMode value for {@code --permission-mode}
 */
@ConfigurationProperties(prefix = "agent.claude")
public record ClaudeProperties(
    String command, Path workspace, Duration timeout, String permissionMode) {

  /** Fails fast at startup on missing settings. */
  public ClaudeProperties {
    if (command == null || command.isBlank()) {
      throw new IllegalArgumentException("agent.claude.command must be set");
    }
    if (workspace == null) {
      throw new IllegalArgumentException("agent.claude.workspace must be set");
    }
    if (timeout == null || timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("agent.claude.timeout must be positive");
    }
    if (permissionMode == null || permissionMode.isBlank()) {
      throw new IllegalArgumentException("agent.claude.permission-mode must be set");
    }
  }
}
