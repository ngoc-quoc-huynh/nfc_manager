.PHONY: style check check-dart test

PUB_FLAGS ?=

style:
	dart format .
	flutter analyze $(PUB_FLAGS)
	dart run dart_code_linter:metrics analyze . --set-exit-on-violation-level=warning

check: check-dart

check-dart:
	dart format --output=none --set-exit-if-changed lib test
	flutter analyze --no-pub lib test
	dart run dart_code_linter:metrics analyze lib test --set-exit-on-violation-level=warning

test:
	flutter test $(PUB_FLAGS) --coverage --test-randomize-ordering-seed=$(or $(seed),random)
	dart run test_cov_console
