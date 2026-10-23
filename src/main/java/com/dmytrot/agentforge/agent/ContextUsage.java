package com.dmytrot.agentforge.agent;

/**
 * How much of the model's context window the conversation occupies.
 *
 * @param usedTokens tokens the conversation takes after the last agent turn
 * @param windowTokens size of the model's context window
 */
public record ContextUsage(long usedTokens, long windowTokens) {

  /**
   * Returns the free part of the window.
   *
   * @return tokens left before the context is full, never negative
   */
  public long remainingTokens() {
    return Math.max(0, windowTokens - usedTokens);
  }

  /**
   * Returns the free part of the window as a percentage.
   *
   * @return whole percent of the window that is still free
   */
  public int percentLeft() {
    return (int) Math.round(100.0 * remainingTokens() / windowTokens);
  }
}
