package com.dmytrot.agentforge.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

/** Extracts the final answer from the line-per-event output of {@code stream-json}. */
class StreamJsonParser {

  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Parses one output line.
   *
   * @param line a single line printed by the CLI
   * @return the reply if this line is the final {@code result} event, otherwise empty
   */
  Optional<AgentReply> parseResult(String line) {
    JsonNode node;
    try {
      node = mapper.readTree(line);
    } catch (JsonProcessingException e) {
      // Anything that is not JSON is noise, not a reason to abort the whole run.
      return Optional.empty();
    }
    if (node == null || !"result".equals(node.path("type").asText())) {
      return Optional.empty();
    }
    String sessionId = node.hasNonNull("session_id") ? node.get("session_id").asText() : null;
    return Optional.of(
        new AgentReply(
            node.path("result").asText(""),
            sessionId,
            node.path("is_error").asBoolean(false),
            parseUsage(node)));
  }

  private static ContextUsage parseUsage(JsonNode result) {
    long window = 0;
    for (JsonNode model : result.path("modelUsage")) {
      window = Math.max(window, model.path("contextWindow").asLong(0));
    }
    // The top-level totals add up every turn of a multi-step task. The last turn is what the
    // next request will carry as context, so that one is the measure of how full the window is.
    JsonNode usage = result.path("usage");
    JsonNode turns = usage.path("iterations");
    JsonNode last = turns.isArray() && !turns.isEmpty() ? turns.get(turns.size() - 1) : usage;
    long used =
        last.path("input_tokens").asLong(0)
            + last.path("cache_creation_input_tokens").asLong(0)
            + last.path("cache_read_input_tokens").asLong(0)
            + last.path("output_tokens").asLong(0);
    return window > 0 && used > 0 ? new ContextUsage(used, window) : null;
  }
}
