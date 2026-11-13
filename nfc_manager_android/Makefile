.PHONY: style check check-dart check-example test android

PUB_FLAGS ?=

style:
	dart format .
	flutter analyze $(PUB_FLAGS)
	dart run dart_code_linter:metrics analyze . --set-exit-on-violation-level=warning

check: check-dart check-example

check-dart:
	dart format --output=none --set-exit-if-changed lib test
	flutter analyze --no-pub lib test
	dart run dart_code_linter:metrics analyze lib test --set-exit-on-violation-level=warning

check-example:
	cd example && dart format --output=none --set-exit-if-changed .
	cd example && flutter analyze --no-pub
	cd example && dart run dart_code_linter:metrics analyze . --set-exit-on-violation-level=warning

test:
	flutter test $(PUB_FLAGS) --coverage --test-randomize-ordering-seed=$(or $(seed),random)
	dart run test_cov_console

android:
	cd example/android && ./gradlew --build-cache :nfc_manager_android:testDebugUnitTest :nfc_manager_android:lintDebug
