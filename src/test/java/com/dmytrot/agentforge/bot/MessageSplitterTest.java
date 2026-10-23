package com.dmytrot.agentforge.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MessageSplitterTest {

  @Test
  void shortTextStaysWhole() {
    assertThat(MessageSplitter.split("hello", 10)).containsExactly("hello");
  }

  @Test
  void prefersLineBreak() {
    assertThat(MessageSplitter.split("aaaa\nbbbb\ncccc", 9)).containsExactly("aaaa\nbbbb", "cccc");
  }

  @Test
  void cutsHardWhenThereIsNoLineBreak() {
    List<String> parts = MessageSplitter.split("abcdefghij", 4);

    assertThat(parts).containsExactly("abcd", "efgh", "ij");
  }
}
