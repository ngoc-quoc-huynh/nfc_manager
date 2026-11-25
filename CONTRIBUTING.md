# Contributing to NFC Manager

Thank you for your interest in contributing to NFC Manager! We welcome community contributions to help improve the
plugin.

## General Guidelines

1. **Read the [code of conduct](CODE_OF_CONDUCT.md).**
2. **Search existing discussions** before creating a new one.
3. **Be respectful and constructive** in all communications.

## Creating a Pull Request

Before creating a pull request, please follow these steps:

1. **Fork the repository**: Create your own fork and clone it to your local machine.
2. **Create your branch**: Start your new branch from `main` (e.g., `git checkout -b feature/your-feature`).
3. **Install the tools**: Install Flutter, JDK 21, and ktlint, then run `flutter pub get` in each package you change.
4. **Add tests**: If you’ve fixed a bug or added new functionality, please include tests.
5. **Format, analyze, and test**: In each package you change, run `make style` and `make test`. For Kotlin changes,
   also run them in `nfc_manager_android/android`.
6. **Create the pull request**: Submit your PR and include a detailed description of the changes made. State whether you
   tested NFC or HCE on a physical device, because unit tests do not cover hardware behavior.
7. **Verify status checks**: Ensure all status checks are passing before finalizing your PR.

## Commit messages

Follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/) for pull request titles:
`type: summary` or `type(scope): summary`.

Use short imperative summaries, scope by package or concern when useful, and use `chore(deps)` for routine dependency
upgrades. Mark breaking changes with `!` or a `BREAKING CHANGE:` footer.

## Discussions

- **Report a bug** with the [bug template](.github/DISCUSSION_TEMPLATE/report-a-bug.yml).
- **Request help** with the [help template](.github/DISCUSSION_TEMPLATE/request-help.yml).
- **Suggest an idea** with the [idea template](.github/DISCUSSION_TEMPLATE/suggest-an-idea.yml).

## License

By contributing to NFC Manager, you agree that your contributions will be licensed under
the [BSD-3-Clause License](LICENSE).
