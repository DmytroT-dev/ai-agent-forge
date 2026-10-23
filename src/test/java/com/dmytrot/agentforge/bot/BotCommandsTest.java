package com.dmytrot.agentforge.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmytrot.agentforge.agent.AgentService;
import com.dmytrot.agentforge.agent.ContextUsage;
import com.dmytrot.agentforge.agent.RateLimits;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BotCommandsTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private final AgentService agent = mock(AgentService.class);
  private final TaskTracker tracker = mock(TaskTracker.class);
  private final BotCommands commands = new BotCommands(agent, tracker, CLOCK);

  @Test
  void newStartsFreshSessionWhenIdle() {
    when(tracker.runningFor()).thenReturn(Optional.empty());

    assertThat(commands.handle("/new")).isEqualTo("Started a new session.");
    verify(agent).startNewSession();
  }

  @Test
  void newIsRefusedWhileTaskRuns() {
    when(tracker.runningFor()).thenReturn(Optional.of(Duration.ofSeconds(5)));

    assertThat(commands.handle("/new")).contains("/stop");
    verify(agent, never()).startNewSession();
  }

  @Test
  void stopCancelsRunningTask() {
    when(tracker.runningFor()).thenReturn(Optional.of(Duration.ofSeconds(5)));

    assertThat(commands.handle("/stop")).isEqualTo("Stop requested.");
    verify(agent).cancel();
  }

  @Test
  void stopWhenIdleDoesNothing() {
    when(tracker.runningFor()).thenReturn(Optional.empty());

    assertThat(commands.handle("/stop")).isEqualTo("Nothing is running.");
    verify(agent, never()).cancel();
  }

  @Test
  void statusShowsTaskSessionAndContext() {
    when(tracker.runningFor()).thenReturn(Optional.of(Duration.ofSeconds(75)));
    when(agent.sessionId()).thenReturn(Optional.of("6c4ff302-898b-42a1-b4f1-78cba3fef9e8"));
    when(agent.contextUsage()).thenReturn(Optional.of(new ContextUsage(34_584, 1_000_000)));
    when(agent.rateLimits())
        .thenReturn(
            Optional.of(
                new RateLimits(
                    new RateLimits.Window(0.28, NOW.plusSeconds(8_100)),
                    new RateLimits.Window(0.04, NOW.plusSeconds(3 * 86_400 + 4 * 3_600)))));

    assertThat(commands.handle("/status"))
        .isEqualTo(
            "Task: running for 1m 15s\n"
                + "Session: 6c4ff302\n"
                + "Context: 34,584 of 1,000,000 tokens used, 965,416 left (97%)\n"
                + "Limits: 5h 28% used, resets in 2h 15m, week 4% used, resets in 3d 4h");
  }

  @Test
  void statusWhenNothingIsKnownYet() {
    when(tracker.runningFor()).thenReturn(Optional.empty());
    when(agent.sessionId()).thenReturn(Optional.empty());
    when(agent.contextUsage()).thenReturn(Optional.empty());

    assertThat(commands.handle("/status"))
        .isEqualTo(
            "Task: idle\nSession: none\nContext: unknown until a task finishes\n"
                + "Limits: unknown until a task finishes");
  }

  @Test
  void acceptsBotNameSuffixFromGroups() {
    when(tracker.runningFor()).thenReturn(Optional.empty());

    assertThat(commands.handle("/stop@my_bot")).isEqualTo("Nothing is running.");
  }

  @Test
  void unknownCommandShowsHelp() {
    assertThat(commands.handle("/wat")).startsWith("Commands:");
  }
}
