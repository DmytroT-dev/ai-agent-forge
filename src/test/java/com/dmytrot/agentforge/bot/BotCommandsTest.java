package com.dmytrot.agentforge.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmytrot.agentforge.agent.AgentService;
import com.dmytrot.agentforge.agent.ContextUsage;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BotCommandsTest {

  private final AgentService agent = mock(AgentService.class);
  private final TaskTracker tracker = mock(TaskTracker.class);
  private final BotCommands commands = new BotCommands(agent, tracker);

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

    assertThat(commands.handle("/status"))
        .isEqualTo(
            "Task: running for 1m 15s\n"
                + "Session: 6c4ff302\n"
                + "Context: 34,584 of 1,000,000 tokens used, 965,416 left (97%)");
  }

  @Test
  void statusWhenNothingIsKnownYet() {
    when(tracker.runningFor()).thenReturn(Optional.empty());
    when(agent.sessionId()).thenReturn(Optional.empty());
    when(agent.contextUsage()).thenReturn(Optional.empty());

    assertThat(commands.handle("/status"))
        .isEqualTo(
            "Task: idle\nSession: none\nContext: unknown until a task finishes");
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
