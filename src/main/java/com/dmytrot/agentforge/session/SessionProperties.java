package com.dmytrot.agentforge.session;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for session persistence.
 *
 * @param file file that holds the current Claude Code session id
 */
@ConfigurationProperties(prefix = "agent.session")
public record SessionProperties(Path file) {}
