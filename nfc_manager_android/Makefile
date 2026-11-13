.PHONY: style check test android

PUB_FLAGS ?=

style:
	dart format .
	flutter analyze $(PUB_FLAGS)
	dart run dart_code_linter:metrics analyze . --set-exit-on-violation-level=warning

check:
	dart format --output=none --set-exit-if-changed .
	flutter analyze $(PUB_FLAGS)
	dart run dart_code_linter:metrics analyze . --set-exit-on-violation-level=warning
	cd example && dart format --output=none --set-exit-if-changed .
	cd example && flutter analyze $(PUB_FLAGS)
	cd example && dart run dart_code_linter:metrics analyze . --set-exit-on-violation-level=warning

test:
	flutter test $(PUB_FLAGS) --coverage --test-randomize-ordering-seed=$(or $(seed),random)
	dart run test_cov_console

android:
	cd example/android && ./gradlew --build-cache :nfc_manager_android:testDebugUnitTest :nfc_manager_android:lintDebug
