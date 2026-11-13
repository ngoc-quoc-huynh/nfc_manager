.PHONY: style test android

style:
	dart format .
	flutter analyze
	dart run dart_code_linter:metrics analyze . --set-exit-on-violation-level=warning

test:
	flutter test --coverage --test-randomize-ordering-seed=$(or $(seed),random)
	dart run test_cov_console

android:
	cd example/android && ./gradlew --build-cache :nfc_manager_android:testDebugUnitTest :nfc_manager_android:lintDebug
