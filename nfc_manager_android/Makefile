.PHONY: style format analyze metrics test pigeon

style: format
	@$(MAKE) --no-print-directory --jobs analyze metrics

format:
	dart format .

analyze:
	flutter analyze

metrics:
	dart run dart_code_linter:metrics analyze . --set-exit-on-violation-level=warning

test:
	flutter test --coverage --test-randomize-ordering-seed=$(or $(seed),random)
	dart run test_cov_console

pigeon:
	dart run pigeon --input pigeons/messages.dart
	dart format lib/src/messages.g.dart
