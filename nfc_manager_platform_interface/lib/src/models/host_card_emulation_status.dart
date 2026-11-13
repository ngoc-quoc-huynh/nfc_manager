/// HCE readiness and command outcomes reported by this plugin.
///
/// These events do not describe persistent authentication state.
enum HostCardEmulationStatus {
  /// The HCE configuration is stored.
  ///
  /// This does not confirm that NFC is enabled or the AID is routed correctly.
  ready,

  /// The command header contains fewer than four bytes.
  invalidCommand,

  /// The declared Lc does not match the command data length, or a SELECT/VERIFY
  /// command is missing required data.
  invalidLcLength,

  /// Indicates that the AID (Application Identifier) has been selected.
  aidSelected,

  /// The received AID did not match the configured AID.
  invalidAid,

  /// The received PIN matched the configured PIN for this command.
  pinVerified,

  /// Indicates that the PIN is incorrect.
  wrongPin,

  /// Indicates that the function is not supported.
  functionNotSupported,

  /// The NFC link ended or the reader switched to another service.
  tagDisconnected,
}
