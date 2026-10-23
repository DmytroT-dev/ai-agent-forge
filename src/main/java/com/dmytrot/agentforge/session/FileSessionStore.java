package com.dmytrot.agentforge.session;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** {@link SessionStore} that keeps the session id in a single text file. */
@Component
public class FileSessionStore implements SessionStore {

  private final Path file;

  /**
   * Creates a store backed by the configured file.
   *
   * @param properties session settings
   */
  public FileSessionStore(SessionProperties properties) {
    this.file = properties.file();
  }

  @Override
  public Optional<String> load() {
    if (!Files.exists(file)) {
      return Optional.empty();
    }
    try {
      String id = Files.readString(file, StandardCharsets.UTF_8).strip();
      return id.isEmpty() ? Optional.empty() : Optional.of(id);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read session file", e);
    }
  }

  @Override
  public void save(String sessionId) {
    try {
      Path parent = file.toAbsolutePath().getParent();
      Files.createDirectories(parent);
      // Write to a temp file first so a crash cannot leave a half-written id behind.
      Path tmp = Files.createTempFile(parent, "session", ".tmp");
      Files.writeString(tmp, sessionId, StandardCharsets.UTF_8);
      Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write session file", e);
    }
  }

  @Override
  public void clear() {
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot delete session file", e);
    }
  }
}
