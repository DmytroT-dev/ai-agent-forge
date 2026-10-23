package com.dmytrot.agentforge.bot;

import com.dmytrot.agentforge.agent.AgentService;
import com.dmytrot.agentforge.agent.ContextUsage;
import com.dmytrot.agentforge.agent.RateLimits;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Handles the slash commands of the bot and produces the text to answer with. */
@Component
public class BotCommands {

  private static final String HELP =
      "Commands:\n/new - start a new session\n/stop - cancel the current task\n"
          + "/continue - carry on with a stopped task\n"
          + "/status - show the task, context usage and limits";

  private final AgentService agent;
  private final TaskTracker tracker;
  private final Clock clock;

  /**
   * Creates the handler.
   *
   * @param agent the coding agent and its session
   * @param tracker tells whether a task is running
   * @param clock time source for "resets in" figures
   */
  public BotCommands(AgentService agent, TaskTracker tracker, Clock clock) {
    this.agent = agent;
    this.tracker = tracker;
    this.clock = clock;
  }

  /**
   * Runs a command.
   *
   * @param text the message text, starting with {@code /}
   * @return the answer to send back
   */
  public String handle(String text) {
    return switch (nameOf(text)) {
      case "/new" -> startNewSession();
      case "/stop" -> stop();
      case "/status" -> status();
      default -> HELP;
    };
  }

  /**
   * Extracts the command name from a message.
   *
   * @param text the message text, starting with {@code /}
   * @return the name with the leading slash, for example {@code /status}
   */
  static String nameOf(String text) {
    String command = text.strip().split("\\s+")[0];
    // In groups Telegram appends the bot name: /status@my_bot.
    int at = command.indexOf('@');
    return at > 0 ? command.substring(0, at) : command;
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
        + "Context: " + describeContext() + "\n"
        + "Limits: " + describeLimits();
  }

  private String describeLimits() {
    Optional<RateLimits> limits = agent.rateLimits();
    if (limits.isEmpty()) {
      return "unknown until a task finishes";
    }
    return "5h " + describeWindow(limits.get().fiveHour())
        + ", week " + describeWindow(limits.get().sevenDay());
  }

  private String describeWindow(RateLimits.Window window) {
    if (window == null) {
      return "n/a";
    }
    Duration untilReset = Duration.between(clock.instant(), window.resetsAt());
    return window.percentUsed() + "% used, resets in " + formatDuration(untilReset);
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
    long seconds = Math.max(0, duration.toSeconds());
    if (seconds < 60) {
      return seconds + "s";
    }
    if (seconds < 3600) {
      return seconds / 60 + "m " + seconds % 60 + "s";
    }
    if (seconds < 86_400) {
      return seconds / 3600 + "h " + seconds % 3600 / 60 + "m";
    }
    return seconds / 86_400 + "d " + seconds % 86_400 / 3600 + "h";
  }
}
