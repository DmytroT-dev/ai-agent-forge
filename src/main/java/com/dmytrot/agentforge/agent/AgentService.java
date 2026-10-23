package com.dmytrot.agentforge.agent;

import com.dmytrot.agentforge.session.SessionStore;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Ties the agent to the stored session, so callers need to know neither. */
@Service
public class AgentService {

  private final AgentRunner runner;
  private final SessionStore sessionStore;
  // Not persisted: it is only known after a task finishes in this run of the application.
  private volatile ContextUsage lastUsage;
  // Belongs to the account, not to a session, so a new session keeps it.
  private volatile RateLimits lastLimits;

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
   * @throws AgentCancelledException if the task was stopped
   * @throws AgentException if the agent could not produce an answer
   */
  public AgentReply ask(String prompt) {
    AgentReply reply;
    try {
      reply = runner.run(prompt, sessionStore.load());
    } catch (AgentCancelledException e) {
      // The stopped task already left its work in the session, so keep it for "continue".
      if (e.sessionId() != null) {
        sessionStore.save(e.sessionId());
      }
      throw e;
    }
    if (reply.sessionId() != null && !reply.error()) {
      sessionStore.save(reply.sessionId());
    }
    if (reply.usage() != null) {
      lastUsage = reply.usage();
    }
    if (reply.limits() != null) {
      lastLimits = reply.limits();
    }
    return reply;
  }

  /** Forgets the current session, so the next task starts from scratch. */
  public void startNewSession() {
    sessionStore.clear();
    lastUsage = null;
  }

  /** Stops the task that is running now, if any. */
  public void cancel() {
    runner.cancel();
  }

  /**
   * Returns the id of the current session.
   *
   * @return the session id, or empty if there is no session yet
   */
  public Optional<String> sessionId() {
    return sessionStore.load();
  }

  /**
   * Returns how full the context of the current session is.
   *
   * @return the usage after the last task, or empty if no task has finished since the session
   *     began in this run of the application
   */
  public Optional<ContextUsage> contextUsage() {
    return Optional.ofNullable(lastUsage);
  }

  /**
   * Returns the subscription limits seen during the last task.
   *
   * @return the limits, or empty if no task has reported them since the application started
   */
  public Optional<RateLimits> rateLimits() {
    return Optional.ofNullable(lastLimits);
  }
}
