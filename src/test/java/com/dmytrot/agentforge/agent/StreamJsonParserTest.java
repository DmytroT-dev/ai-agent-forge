package com.dmytrot.agentforge.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StreamJsonParserTest {

  private final StreamJsonParser parser = new StreamJsonParser();

  @Test
  void parsesResultEvent() {
    String line =
        "{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":false,"
            + "\"result\":\"Hi there\",\"session_id\":\"abc-123\"}";

    assertThat(parser.parseResult(line))
        .contains(new AgentReply("Hi there", "abc-123", false));
  }

  @Test
  void marksErrorResult() {
    String line = "{\"type\":\"result\",\"is_error\":true,\"result\":\"boom\"}";

    assertThat(parser.parseResult(line)).contains(new AgentReply("boom", null, true));
  }

  @Test
  void ignoresOtherEvents() {
    assertThat(parser.parseResult("{\"type\":\"assistant\",\"message\":{}}")).isEmpty();
  }

  @Test
  void ignoresNonJsonLines() {
    assertThat(parser.parseResult("not json at all")).isEmpty();
    assertThat(parser.parseResult("")).isEmpty();
  }
}
