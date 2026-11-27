.PHONY: style test

KOTLIN_FILES := '**/*.{kt,kts}' '!**/*.g.kt' '!**/build/**' '!**/.gradle/**'

# The example's Gradle build includes this plugin as a subproject. Its
# `flutter pub get` writes the Flutter SDK path that Gradle needs.
GRADLE := cd ../example && flutter pub get && cd android && ./gradlew

style:
	ktlint --format $(KOTLIN_FILES)
	$(GRADLE) :nfc_manager_android:lintDebug

test:
	$(GRADLE) :nfc_manager_android:testDebugUnitTest
