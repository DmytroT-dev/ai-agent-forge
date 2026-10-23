package com.dmytrot.agentforge.bot;

import com.dmytrot.agentforge.agent.AgentService;
import com.dmytrot.agentforge.agent.ContextUsage;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Handles the slash commands of the bot and produces the text to answer with. */
@Component
public class BotCommands {

  private static final String HELP =
      "Commands:\n/new - start a new session\n/stop - cancel the current task\n"
          + "/status - show the task and the context usage";

  private final AgentService agent;
  private final TaskTracker tracker;

  /**
   * Creates the handler.
   *
   * @param agent the coding agent and its session
   * @param tracker tells whether a task is running
   */
  public BotCommands(AgentService agent, TaskTracker tracker) {
    this.agent = agent;
    this.tracker = tracker;
  }

  /**
   * Runs a command.
   *
   * @param text the message text, starting with {@code /}
   * @return the answer to send back
   */
  public String handle(String text) {
    String command = text.strip().split("\\s+")[0];
    // In groups Telegram appends the bot name: /status@my_bot.
    int at = command.indexOf('@');
    if (at > 0) {
      command = command.substring(0, at);
    }
    return switch (command) {
      case "/new" -> startNewSession();
      case "/stop" -> stop();
      case "/status" -> status();
      default -> HELP;
    };
  }

  private String startNewSession() {
    // Clearing the session under a running task would be overwritten when that task finishes.
    if (tracker.runningFor().isPresent()) {
      return "A task is running. Send /stop first.";
    }
    agent.startNewSession();
    return "Started a new session.";
  }

  private String stop() {
    if (tracker.runningFor().isEmpty()) {
      return "Nothing is running.";
    }
    agent.cancel();
    return "Stop requested.";
  }

  private String status() {
    return "Task: " + describeTask() + "\n"
        + "Session: " + describeSession() + "\n"
        + "Context: " + describeContext();
  }

  private String describeTask() {
    return tracker
        .runningFor()
        .map(elapsed -> "running for " + formatDuration(elapsed))
        .orElse("idle");
  }

  private String describeSession() {
    return agent
        .sessionId()
        // Eight characters identify a session well enough and keep the full id out of the chat.
        .map(id -> id.substring(0, Math.min(8, id.length())))
        .orElse("none");
  }

  private String describeContext() {
    Optional<ContextUsage> usage = agent.contextUsage();
    if (usage.isEmpty()) {
      return "unknown until a task finishes";
    }
    ContextUsage u = usage.get();
    return String.format(
        Locale.US,
        "%,d of %,d tokens used, %,d left (%d%%)",
        u.usedTokens(),
        u.windowTokens(),
        u.remainingTokens(),
        u.percentLeft());
  }

  private static String formatDuration(Duration duration) {
    long seconds = duration.toSeconds();
    return seconds < 60 ? seconds + "s" : seconds / 60 + "m " + seconds % 60 + "s";
  }
}
