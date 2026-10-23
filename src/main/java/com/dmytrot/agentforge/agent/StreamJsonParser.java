package com.dmytrot.agentforge.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;

/** Reads the events of the line-per-event output of {@code stream-json}. */
class StreamJsonParser {

  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Parses the final answer.
   *
   * @param line a single line printed by the CLI
   * @return the reply if this line is the final {@code result} event, otherwise empty
   */
  Optional<AgentReply> parseResult(String line) {
    return read(line)
        .filter(n -> "result".equals(n.path("type").asText()))
        .map(
            n ->
                new AgentReply(
                    n.path("result").asText(""),
                    n.hasNonNull("session_id") ? n.get("session_id").asText() : null,
                    n.path("is_error").asBoolean(false),
                    parseUsage(n),
                    null));
  }

  /**
   * Reads the session id from the first event of a run.
   *
   * @param line a single line printed by the CLI
   * @return the session id if this is the {@code init} event, otherwise empty
   */
  Optional<String> parseInitSessionId(String line) {
    return read(line)
        .filter(n -> "system".equals(n.path("type").asText()))
        .filter(n -> "init".equals(n.path("subtype").asText()))
        .filter(n -> n.hasNonNull("session_id"))
        .map(n -> n.get("session_id").asText());
  }

  /**
   * Reads the subscription limits from a rate limit event.
   *
   * @param line a single line printed by the CLI
   * @return the limits if this is a {@code rate_limit_event} that carries any window
   */
  Optional<RateLimits> parseRateLimits(String line) {
    return read(line)
        .filter(n -> "rate_limit_event".equals(n.path("type").asText()))
        .map(n -> n.path("rate_limit_info").path("unifiedWindows"))
        .map(w -> new RateLimits(window(w.path("five_hour")), window(w.path("seven_day"))))
        .filter(limits -> limits.fiveHour() != null || limits.sevenDay() != null);
  }

  private Optional<JsonNode> read(String line) {
    try {
      return Optional.ofNullable(mapper.readTree(line));
    } catch (JsonProcessingException e) {
      // Anything that is not JSON is noise, not a reason to abort the whole run.
      return Optional.empty();
    }
  }

  private static RateLimits.Window window(JsonNode node) {
    if (!node.hasNonNull("utilization") || !node.hasNonNull("resetsAt")) {
      return null;
    }
    return new RateLimits.Window(
        node.get("utilization").asDouble(), Instant.ofEpochSecond(node.get("resetsAt").asLong()));
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
