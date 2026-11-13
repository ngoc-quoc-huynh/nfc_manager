# Contributing to NFC Manager

Thank you for your interest in contributing to NFC Manager! We welcome community contributions to help improve the plugin.

## General Guidelines

1. **Read the [code of conduct](CODE_OF_CONDUCT.md)**
    - Familiarize yourself with our expectations for community behavior.
2. **Search existing discussions**:
    - Before creating a new discussion, check for existing conversations to avoid duplicates and keep our discussions
      focused.
3. **Be respectful and constructive**:
    - Maintain a positive and respectful tone in all communications. Constructive feedback is essential for a healthy
      community.
4. **Use the appropriate template**:
    - When reporting a bug, request for help, suggesting an idea, or creating a pull request, ensure you
      use the relevant template to provide all necessary information.

## Creating a Pull Request

Before creating a pull request, please follow these steps:

1. **Fork the repository**: Create your own fork and clone it to your local machine.
2. **Create your branch**: Start your new branch from `main` (e.g., `git checkout -b feature/your-feature`).
3. **Set up your environment**: Follow [Development setup](#development-setup).
4. **Squash your commits**: Combine your commits into a single, meaningful commit message.
5. **Add tests**: If you’ve fixed a bug or added new functionality, please include tests.
6. **Run tests**: Ensure the test suite passes before submitting your PR.
7. **Format and analyze your code**: Run `make style` to format your code and `make check` to confirm it passes.
8. **Create the pull request**: Submit your PR and include a detailed description of the changes made.
9. **Verify status checks**: Ensure all status checks are passing before finalizing your PR.

## Development setup

The repository pins Flutter and Java in `mise.toml`. Install [mise](https://mise.jdx.dev), then run once
from the repository root:

```sh
mise install
```

In the package you changed (`nfc_manager`, `nfc_manager_android` or `nfc_manager_platform_interface`), install its
dependencies first, as CI does:

```sh
flutter pub get --no-example
(cd example && flutter pub get --enforce-lockfile)  # packages with an example only
```

Then run the commands below in that package:

- `make style` formats your code and analyzes it. It modifies files. In `nfc_manager_android` it also formats Kotlin
  with ktlint (installed by `mise install`).
- `make check` runs the same formatting and analysis checks as CI without modifying files, including the ktlint check
  in `nfc_manager_android`.
- `make test` runs the tests. Pass `seed=1` to reproduce a test order.
- `make android` runs the Android unit tests and lint. It exists in `nfc_manager_android` only and needs a local
  Android SDK (Java comes from mise).

Run these from the package directory with `mise exec -- make check` if mise is not activated in your shell.

## Commit messages

Follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/) for final squashed commits:
`type: summary` or `type(scope): summary`.

Use short imperative summaries, scope by package or concern when useful, and use `chore(deps)` for routine dependency
upgrades. Mark breaking changes with `!` or a `BREAKING CHANGE:` footer.

## Report a bug

When reporting a bug, please use the provided [template](.github/DISCUSSION_TEMPLATE/report-a-bug.yml) to ensure that
all necessary information is included. This template is designed to guide you through providing the details needed to
understand and address the issue effectively.

- **Plugin version**: Specify the version of the plugin where you encountered the bug. This helps us understand if the issue
  is version-specific.
- **Flutter version**: Specify the Flutter version you are using, e.g. the output of `flutter --version`.
- **Android version**: Specify the Android version of the device you are testing on.
- **Device**: Specify the device manufacturer and model.
- **Mode**: State whether the bug occurs in reader mode, host card emulation (HCE), or both.
- **Description**: Give a clear and detailed description of the bug. The more specific you are, the easier it will be
  for us to understand the issue.
- **Steps to reproduce**: List the steps you took when you encountered the bug. This should be a clear, step-by-step
  process to help us replicate the issue.
- **Expected behavior**: Describe what you expected to happen when you followed the steps above. This helps clarify the
  discrepancy between expected and actual behavior.
- **Screenshots**: If possible, include links to screenshots or images that illustrate the issue. Visual evidence can be
  very helpful in quickly identifying problems.
- **Additional information**: Provide any other context or information that might be relevant. This could include error
  logs, console output, or conditions under which the bug does not occur.

## Requesting help

When requesting help, please use the provided [template](.github/DISCUSSION_TEMPLATE/request-help.yml) to ensure that
all necessary information is included. This template is designed to guide you in explaining your question clearly and
effectively.

- **Detail your question**: Describe your issue in detail, explaining what you are trying to achieve and where you are
  stuck.
- **Include code snippets**: If relevant, provide code snippets to give context.

## Suggest an idea

When suggesting an idea, please use the provided [template](.github/DISCUSSION_TEMPLATE/suggest-an-idea.yml) to ensure
that all necessary information is included. This template is designed to guide you in articulating your suggestion
clearly and effectively.

- **Clearly articulate your idea**: Provide a clear and concise description of your suggestion, including how it would
  improve the plugin.
- **Explain use cases**: Share specific scenarios where your idea would be useful.
- **Be open to feedback**: Encourage discussion and be willing to refine your idea.

---

By following these guidelines and utilizing the provided templates, you contribute to a more organized and efficient
process for our community. Your insights and contributions are invaluable in helping us improve the plugin and create a
better experience for all users. Thank you for your support and dedication!

## License

By contributing to NFC Manager, you agree that your contributions will be licensed under
the [BSD-3-Clause License](LICENSE).
