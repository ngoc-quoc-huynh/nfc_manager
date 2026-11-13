// Declares names that package:nfc_manager must not export. public_api_test.dart
// imports this library next to the barrel, so an export of any of these names
// makes the reference ambiguous and the test fails to compile. Only the names
// matter.

/// Stands in for the platform interface class.
final class NfcManagerPlatform {}

/// Stands in for the platform interface AID validator.
const checkAidLength = 'checkAidLength';

/// Stands in for the platform interface PIN validator.
const checkPinLength = 'checkPinLength';
