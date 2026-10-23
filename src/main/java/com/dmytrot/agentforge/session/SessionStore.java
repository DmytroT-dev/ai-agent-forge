package com.dmytrot.agentforge.session;

import java.util.Optional;

/** Persists the Claude Code session id so a conversation survives bot restarts. */
public interface SessionStore {

  /**
   * Returns the stored session id.
   *
   * @return the session id, or empty if no session was saved yet
   */
  Optional<String> load();

  /**
   * Replaces the stored session id.
   *
   * @param sessionId id to pass to {@code claude --resume}
   */
  void save(String sessionId);

  /** Forgets the stored session id, so the next task starts a new session. */
  void clear();
}
