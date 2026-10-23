package com.dmytrot.agentforge.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmytrot.agentforge.session.SessionStore;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentServiceTest {

  private final InMemoryStore store = new InMemoryStore();

  @Test
  void firstTaskStartsNewSessionAndSavesItsId() {
    FakeRunner runner = new FakeRunner(new AgentReply("ok", "s1", false));

    new AgentService(runner, store).ask("hello");

    assertThat(runner.lastSession).isEmpty();
    assertThat(store.load()).contains("s1");
  }

  @Test
  void nextTaskResumesSavedSession() {
    store.save("s1");
    FakeRunner runner = new FakeRunner(new AgentReply("ok", "s1", false));

    new AgentService(runner, store).ask("again");

    assertThat(runner.lastSession).contains("s1");
  }

  @Test
  void failedRunDoesNotOverwriteSession() {
    store.save("s1");
    FakeRunner runner = new FakeRunner(new AgentReply("boom", "s2", true));

    new AgentService(runner, store).ask("task");

    assertThat(store.load()).contains("s1");
  }

  private static final class FakeRunner implements AgentRunner {

    private final AgentReply reply;
    private Optional<String> lastSession;

    FakeRunner(AgentReply reply) {
      this.reply = reply;
    }

    @Override
    public AgentReply run(String prompt, Optional<String> sessionId) {
      lastSession = sessionId;
      return reply;
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
