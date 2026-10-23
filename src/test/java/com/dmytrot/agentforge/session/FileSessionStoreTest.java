package com.dmytrot.agentforge.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSessionStoreTest {

  @TempDir Path dir;

  private FileSessionStore store;

  @BeforeEach
  void setUp() {
    store = new FileSessionStore(new SessionProperties(dir.resolve("nested/session-id")));
  }

  @Test
  void loadReturnsEmptyWhenNothingSaved() {
    assertThat(store.load()).isEmpty();
  }

  @Test
  void savedIdIsLoadedBack() {
    store.save("abc-123");

    assertThat(store.load()).contains("abc-123");
  }

  @Test
  void saveOverwritesPreviousId() {
    store.save("first");
    store.save("second");

    assertThat(store.load()).contains("second");
  }

  @Test
  void clearForgetsTheId() {
    store.save("abc-123");
    store.clear();

    assertThat(store.load()).isEmpty();
  }
}
