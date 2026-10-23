package com.dmytrot.agentforge.agent;

import com.dmytrot.agentforge.session.SessionStore;
import org.springframework.stereotype.Service;

/** Ties the agent to the stored session, so callers need to know neither. */
@Service
public class AgentService {

  private final AgentRunner runner;
  private final SessionStore sessionStore;

  /**
   * Creates the service.
   *
   * @param runner launches the agent
   * @param sessionStore remembers the session between tasks
   */
  public AgentService(AgentRunner runner, SessionStore sessionStore) {
    this.runner = runner;
    this.sessionStore = sessionStore;
  }

  /**
   * Runs a task in the current session and remembers the session for the next one.
   *
   * @param prompt what the user asked
   * @return the agent's answer
   * @throws AgentException if the agent could not produce an answer
   */
  public AgentReply ask(String prompt) {
    AgentReply reply = runner.run(prompt, sessionStore.load());
    if (reply.sessionId() != null && !reply.error()) {
      sessionStore.save(reply.sessionId());
    }
    return reply;
  }
}
