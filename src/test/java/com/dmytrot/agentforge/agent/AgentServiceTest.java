package com.dmytrot.agentforge.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmytrot.agentforge.session.SessionStore;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentServiceTest {

  private final InMemoryStore store = new InMemoryStore();

  @Test
  void firstTaskStartsNewSessionAndSavesItsId() {
    FakeRunner runner = new FakeRunner(new AgentReply("ok", "s1", false, null));

    new AgentService(runner, store).ask("hello");

    assertThat(runner.lastSession).isEmpty();
    assertThat(store.load()).contains("s1");
  }

  @Test
  void nextTaskResumesSavedSession() {
    store.save("s1");
    FakeRunner runner = new FakeRunner(new AgentReply("ok", "s1", false, null));

    new AgentService(runner, store).ask("again");

    assertThat(runner.lastSession).contains("s1");
  }

  @Test
  void failedRunDoesNotOverwriteSession() {
    store.save("s1");
    FakeRunner runner = new FakeRunner(new AgentReply("boom", "s2", true, null));

    new AgentService(runner, store).ask("task");

    assertThat(store.load()).contains("s1");
  }

  @Test
  void remembersContextUsageOfLastTask() {
    ContextUsage usage = new ContextUsage(1_000, 10_000);
    FakeRunner runner = new FakeRunner(new AgentReply("ok", "s1", false, usage));
    AgentService service = new AgentService(runner, store);

    service.ask("hello");

    assertThat(service.contextUsage()).contains(usage);
  }

  @Test
  void newSessionClearsIdAndUsage() {
    FakeRunner runner =
        new FakeRunner(new AgentReply("ok", "s1", false, new ContextUsage(1, 10)));
    AgentService service = new AgentService(runner, store);
    service.ask("hello");

    service.startNewSession();

    assertThat(service.sessionId()).isEmpty();
    assertThat(service.contextUsage()).isEmpty();
  }

  @Test
  void cancelIsPassedToRunner() {
    FakeRunner runner = new FakeRunner(null);

    new AgentService(runner, store).cancel();

    assertThat(runner.cancelled).isTrue();
  }

  private static final class FakeRunner implements AgentRunner {

    private final AgentReply reply;
    private Optional<String> lastSession;
    private boolean cancelled;

    FakeRunner(AgentReply reply) {
      this.reply = reply;
    }

    @Override
    public AgentReply run(String prompt, Optional<String> sessionId) {
      lastSession = sessionId;
      return reply;
    }

    @Override
    public void cancel() {
      cancelled = true;
    }
  }

  private static final class InMemoryStore implements SessionStore {

    private String id;

    @Override
    public Optional<String> load() {
      return Optional.ofNullable(id);
    }

    @Override
    public void save(String sessionId) {
      id = sessionId;
    }

    @Override
    public void clear() {
      id = null;
    }
  }
}
