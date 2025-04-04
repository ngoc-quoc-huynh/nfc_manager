# nfc_manager_platform_interface

A common platform interface for the [`nfc_manager`](../nfc_manager) plugin.

This interface allows platform-specific implementations of the `nfc_manager`
plugin, as well as the plugin itself, to ensure they are supporting the same interface.

# Usage

To implement a new platform-specific implementation of `nfc_manager`, extend
[`NfcManagerPlatform`](lib/src/platform.dart) with an implementation that performs the platform-specific
behavior, and when you register your plugin, set the default
`NfcManagerPlatform` by calling
`NfcManagerPlatform.instance = NfcManagerExamplePlatform()`.

# Note on breaking changes

Strongly prefer non-breaking changes (such as adding a method to the interface)
over breaking changes for this package.

See https://flutter.dev/go/platform-interface-breaking-changes for a discussion on why a less-clean
interface is preferable to a breaking change.
