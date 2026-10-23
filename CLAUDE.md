# ai-agent-forge

## Project goal
A Telegram bot that lets me chat with an AI agent that writes code 24/7, the same way I use Claude CLI.
The bot receives my Telegram messages, runs Claude Code CLI in headless mode as an external process,
and streams the output back to the chat.

## Tech stack
- Java 21, Spring Boot, Maven
- Telegram library: `telegrambots` (rubenlagus)
- Agent engine: Claude Code CLI launched via `ProcessBuilder`,
  e.g. `claude -p "<prompt>" --output-format stream-json`
- Sessions: `--resume <session_id>`; the session id is persisted to disk or a database
- Development: locally in IntelliJ IDEA
- Hosting: Ubuntu VPS, systemd service
- CI/CD: GitHub Actions builds the jar on merge to `main` and deploys it to the server

## Code style
- Follow the Google Java Style Guide strictly (https://google.github.io/styleguide/javaguide.html):
    - 2-space indentation, no tabs
    - 100-character line limit
    - Braces always required, K&R style
    - No wildcard imports; static imports first, then non-static, both in ASCII order
    - Class names `UpperCamelCase`, methods and variables `lowerCamelCase`, constants `UPPER_SNAKE_CASE`
    - One top-level class per file
- Write Javadoc for every public class and every public method.
- Prefer small, focused classes and methods. Prefer constructor injection over field injection.
- Do not leave dead code, commented-out code, or unused imports.

## Comments and language
- ALL code comments, Javadoc, log messages, commit messages, PR titles and descriptions
  must be written in English. Always, without exceptions.
- Comments explain WHY, not WHAT. Do not restate what the code obviously does.
- Identifiers (classes, methods, variables, packages) are in English too.

## Secrets and sensitive data
- Never hardcode secrets: Telegram bot token, GitHub tokens, Anthropic API keys, SSH keys,
  passwords, user IDs, server addresses.
- Secrets live only in environment variables or in git-ignored local files such as
  `application-local.yml` and `.env`.
- Reference secrets in code only through configuration properties, for example
  `${TELEGRAM_BOT_TOKEN}`.
- Every file that can contain a secret MUST be listed in `.gitignore` BEFORE it is created.
  Before every commit, check `git status` and `git diff --cached` for anything that looks like a secret,
  and run `gitleaks protect --staged` if it is installed.
- Never print secrets in logs, error messages, Telegram replies, PR descriptions, or commit messages.
- Provide an `application-example.yml` with placeholder values so the required configuration
  is documented without exposing real values.
- Secrets must never be deployed from the repository. The CI pipeline builds the jar from a clean
  git checkout (so git-ignored files cannot end up in it). On the server, secrets are provided
  by an environment file outside the project directory, readable only by the service user.
- If a secret is ever committed by mistake: stop, tell me immediately, and do not try to hide it
  with a follow-up commit. The secret must be rotated.

## Git workflow
- NEVER commit or push directly to `main`.
- Create a separate branch for every task: `agent/<short-kebab-case-description>`.
- Keep commits small and focused. Use clear, imperative commit messages
  (for example: `Add whitelist check for Telegram user id`).
- Before opening a PR, run the build and the tests (`mvn -B verify`) and make sure they pass.
- Push the branch and open a PR with `gh pr create`. The PR description must contain:
  what was changed, why, how to verify it, and anything that was not finished or is uncertain.
- Do NOT merge PRs yourself. I review every PR and merge it manually.
- Do NOT delete branches you did not create. Do NOT force-push shared branches.
- Do NOT modify `.github/workflows/*`, deployment scripts, or this file without asking me first.

## Security rules
- The bot responds ONLY to my Telegram user id (whitelist). Messages from anyone else are ignored.
- The agent works only inside its dedicated workspace directory, which is separate from
  this repository.
- Never run destructive commands (`rm -rf`, `git reset --hard`, dropping databases, etc.)
  without my explicit confirmation.
- `--dangerously-skip-permissions` is allowed only inside an isolated environment: a container
  or a dedicated unprivileged OS user with no access to secrets or to this repository's credentials.
- Validate and sanitize everything that comes from Telegram before passing it to a process.
- Never build shell commands by string concatenation with user input. Pass arguments
  as a list to `ProcessBuilder`.

## Planned bot commands
`/new` (start a new session), `/stop` (cancel the current task), `/status` (show the current task),
confirm/reject buttons for risky actions, and a push notification when a long task finishes.

## Current status
Update this section in every PR that changes the project state.

- GitHub repository created and public, `main` protected by a ruleset (pull request required).
- `.gitignore` added.
- Next step: create the Spring Boot project skeleton and a simple echo bot,
  then run the first deploy through the full pipeline.