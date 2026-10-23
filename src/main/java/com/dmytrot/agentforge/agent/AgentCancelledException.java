package com.dmytrot.agentforge.agent;

/** The user stopped the task, so there is no answer to wait for. */
public class AgentCancelledException extends AgentException {

  /** Creates the exception. */
  public AgentCancelledException() {
    super("The task was stopped");
  }
}
