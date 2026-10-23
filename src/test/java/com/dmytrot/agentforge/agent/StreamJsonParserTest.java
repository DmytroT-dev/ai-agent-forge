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
        .contains(new AgentReply("Hi there", "abc-123", false, null));
  }

  @Test
  void marksErrorResult() {
    String line = "{\"type\":\"result\",\"is_error\":true,\"result\":\"boom\"}";

    assertThat(parser.parseResult(line)).contains(new AgentReply("boom", null, true, null));
  }

  @Test
  void measuresContextByTheLastTurn() {
    String line =
        "{\"type\":\"result\",\"result\":\"x\",\"usage\":{\"input_tokens\":999,"
            + "\"iterations\":[{\"input_tokens\":1,\"cache_creation_input_tokens\":10,"
            + "\"cache_read_input_tokens\":100,\"output_tokens\":5},"
            + "{\"input_tokens\":2,\"cache_creation_input_tokens\":20,"
            + "\"cache_read_input_tokens\":200,\"output_tokens\":8}]},"
            + "\"modelUsage\":{\"m\":{\"contextWindow\":1000000}}}";

    assertThat(parser.parseResult(line).orElseThrow().usage())
        .isEqualTo(new ContextUsage(230, 1_000_000));
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
