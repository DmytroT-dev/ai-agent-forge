package com.dmytrot.agentforge.agent;

/** The agent could not produce a reply: the process failed to start, timed out or gave no result. */
public class AgentException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message what went wrong
   */
  public AgentException(String message) {
    super(message);
  }

  /**
   * Creates the exception with a cause.
   *
   * @param message what went wrong
   * @param cause underlying failure
   */
  public AgentException(String message, Throwable cause) {
    super(message, cause);
  }
}
