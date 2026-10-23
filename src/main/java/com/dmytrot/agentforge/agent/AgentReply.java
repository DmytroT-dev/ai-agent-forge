package com.dmytrot.agentforge.agent;

/**
 * Final answer of one agent run.
 *
 * @param text answer text to show to the user
 * @param sessionId session to resume next time, may be {@code null} if the CLI did not report one
 * @param error {@code true} if the CLI reported the run as failed
 * @param usage context usage after this run, may be {@code null} if the CLI did not report it
 */
public record AgentReply(String text, String sessionId, boolean error, ContextUsage usage) {}
