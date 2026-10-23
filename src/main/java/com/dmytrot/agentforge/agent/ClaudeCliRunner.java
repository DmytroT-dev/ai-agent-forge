package com.dmytrot.agentforge.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** {@link AgentRunner} that launches Claude Code CLI in headless mode as a child process. */
@Component
public class ClaudeCliRunner implements AgentRunner {

  private static final Pattern SAFE_SESSION_ID = Pattern.compile("[A-Za-z0-9-]{1,100}");

  private final ClaudeProperties properties;
  private final StreamJsonParser parser = new StreamJsonParser();
  private final AtomicBoolean cancelled = new AtomicBoolean();
  private volatile Process current;

  /**
   * Creates the runner.
   *
   * @param properties CLI settings
   */
  public ClaudeCliRunner(ClaudeProperties properties) {
    this.properties = properties;
    try {
      Files.createDirectories(properties.workspace());
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create the agent workspace", e);
    }
  }

  @Override
  public AgentReply run(String prompt, Optional<String> sessionId) {
    cancelled.set(false);
    Process process = start(sessionId);
    current = process;
    // The prompt goes through stdin, never through arguments: user text cannot be mistaken for
    // an option or reach a shell, and it is not limited by the command line length.
    try (var stdin = process.getOutputStream()) {
      stdin.write(prompt.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      kill(process);
      throw new AgentException("Cannot pass the prompt to the agent", e);
    }
    CompletableFuture<Optional<AgentReply>> output =
        CompletableFuture.supplyAsync(() -> readResult(process));
    try {
      Optional<AgentReply> result =
          output.get(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
      if (cancelled.get()) {
        throw new AgentCancelledException();
      }
      return result.orElseThrow(() -> new AgentException("The agent finished without a result"));
    } catch (TimeoutException e) {
      throw new AgentException("The agent did not finish in " + properties.timeout(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AgentException("Interrupted while waiting for the agent", e);
    } catch (ExecutionException e) {
      throw new AgentException("Cannot read the agent output", e.getCause());
    } finally {
      current = null;
      kill(process);
    }
  }

  @Override
  public void cancel() {
    Process process = current;
    if (process != null) {
      cancelled.set(true);
      kill(process);
    }
  }

  /** Kills the process together with its children: on Windows the CLI runs behind a shim. */
  private static void kill(Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }

  private Process start(Optional<String> sessionId) {
    List<String> command = new ArrayList<>();
    command.add(properties.command());
    command.addAll(
        List.of(
            "-p",
            "--output-format",
            "stream-json",
            "--verbose",
            "--permission-mode",
            properties.permissionMode()));
    sessionId.ifPresent(
        id -> {
          // The id comes from a file on disk, so it is checked before it becomes an argument.
          if (!SAFE_SESSION_ID.matcher(id).matches()) {
            throw new AgentException("Stored session id has an unexpected format");
          }
          command.addAll(List.of("--resume", id));
        });
    try {
      return new ProcessBuilder(command)
          .directory(properties.workspace().toFile())
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
    } catch (IOException e) {
      throw new AgentException("Cannot start the agent process", e);
    }
  }

  private Optional<AgentReply> readResult(Process process) {
    try (var reader =
        new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      Optional<AgentReply> result = Optional.empty();
      String line;
      while ((line = reader.readLine()) != null) {
        Optional<AgentReply> parsed = parser.parseResult(line);
        if (parsed.isPresent()) {
          result = parsed;
        }
      }
      return result;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
