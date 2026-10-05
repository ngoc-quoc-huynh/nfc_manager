# Development Guidelines

## Approach

- Prefer simple, readable solutions and small, focused changes.
- Use modern, established practices compatible with the project's
  supported SDKs and platforms.
- Treat existing conventions as a starting point. Improve them when
  the requested work benefits from a simpler, safer, or more
  maintainable design. Apply improvements consistently within the
  affected code and report material convention changes.
- Reuse existing code and tools when appropriate. Avoid speculative
  features, unrelated refactoring, and premature optimization.
- Validate untrusted input, including NFC tag data and APDUs.
  Keep secrets and sensitive real data out of code, logs, and Git.

## Architecture

- Keep responsibilities clear, public APIs small, and implementation
  details private.
- Separate platform-specific integration from shared logic through
  clear interfaces.
- Introduce layers or abstractions only when a concrete requirement
  justifies them.

## Workflow

- Inspect relevant code, instructions, and tests before making changes.
- Follow CONTRIBUTING.md when preparing requested contributions.
- Complete clear requests without unnecessary questions. Ask when
  ambiguity materially affects scope, safety, or design.
- Keep reviews and diagnoses read-only unless changes are requested.
- Preserve unrelated user changes and keep dependency changes within
  the requested scope.
- Avoid creating temporary files or directories. Ask the user first
  if temporary scripts, logs, backups, or test copies are needed.
- Ask before adding dependencies, making incompatible public API or
  cross-package/platform contract changes, raising SDK or platform
  minimums, or taking destructive actions unless explicitly authorized.
- Commit, push, publish, or deploy only when explicitly requested.

## Verification

- Add or update focused tests when behavior changes.
- Use the project's configured formatting, linting, and testing tools.
  Run checks appropriate to the change, including dependent packages
  when shared contracts change, and review the final diff.
- Never weaken or bypass tests, analyzer rules, or CI checks to pass.
- Report physical NFC/HCE verification separately from unit tests.
  State when relevant hardware behavior remains unverified.
- Report changes, checks actually performed, failures, and anything
  unverified. Keep the report concise and practical.
