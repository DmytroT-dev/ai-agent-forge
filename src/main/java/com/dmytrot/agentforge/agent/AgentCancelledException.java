package com.dmytrot.agentforge.agent;

/** The user stopped the task, so there is no answer to wait for. */
public class AgentCancelledException extends AgentException {

  private final String sessionId;

  /**
   * Creates the exception.
   *
   * @param sessionId session the stopped task ran in, {@code null} if the CLI never reported it
   */
  public AgentCancelledException(String sessionId) {
    super("The task was stopped");
    this.sessionId = sessionId;
  }

  /**
   * Returns the session of the stopped task.
   *
   * @return the session id, or {@code null} if it is unknown
   */
  public String sessionId() {
    return sessionId;
  }
}
