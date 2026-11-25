/// HCE readiness and command outcomes reported by this plugin.
///
/// These events do not describe persistent authentication state.
enum HostCardEmulationStatus {
  /// The HCE configuration is stored.
  ///
  /// This does not confirm that NFC is enabled or the AID is routed correctly.
  ready,

  /// The command is shorter than the 4-byte header.
  invalidCommand,

  /// The Lc byte is `00` or does not match the command data, or a
  /// SELECT/VERIFY command has no data.
  invalidLcLength,

  /// The received AID matched the configured AID.
  aidSelected,

  /// The received AID did not match the configured AID.
  invalidAid,

  /// The received PIN matched the configured PIN.
  pinVerified,

  /// The received PIN did not match. The platform may end the stream after
  /// repeated wrong PINs.
  wrongPin,

  /// The command is not SELECT AID (`A4 04`) or VERIFY (`20 00`).
  functionNotSupported,

  /// The NFC link ended or the reader switched to another service.
  tagDisconnected,
}
