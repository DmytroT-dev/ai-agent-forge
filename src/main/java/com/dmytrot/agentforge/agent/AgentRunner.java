package com.dmytrot.agentforge.agent;

import java.util.Optional;

/** Runs one task on the coding agent. Hides how the agent is actually launched. */
public interface AgentRunner {

  /**
   * Runs a task and waits for the final answer.
   *
   * @param prompt what the user asked
   * @param sessionId session to continue, or empty to start a new one
   * @return the agent's final answer
   * @throws AgentException if the agent could not produce an answer
   */
  AgentReply run(String prompt, Optional<String> sessionId);
}
