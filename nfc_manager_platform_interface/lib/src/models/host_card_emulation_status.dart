/// Represents the status of the host card emulation (HCE) operation.
enum HostCardEmulationStatus {
  /// Indicates that the HCE is ready for operations.
  ready,

  /// Indicates that the command is invalid because it must be at least 4 bytes
  /// long.
  invalidCommand,

  /// Indicates that the length of the command data is invalid.
  invalidLcLength,

  /// Indicates that the AID (Application Identifier) has been selected.
  aidSelected,

  /// Indicates that the AID is invalid.
  invalidAid,

  /// Indicates that the PIN has been verified.
  pinVerified,

  /// Indicates that the PIN is incorrect.
  wrongPin,

  /// Indicates that the function is not supported.
  functionNotSupported,

  /// Indicates that the tag is disconnected.
  tagDisconnected;

  /// Constructs a [HostCardEmulationStatus] from a string representation.
  factory HostCardEmulationStatus.fromString(String status) => switch (status) {
    'READY' => HostCardEmulationStatus.ready,
    'INVALID_COMMAND' => HostCardEmulationStatus.invalidCommand,
    'INVALID_LC_LENGTH' => HostCardEmulationStatus.invalidLcLength,
    'AID_SELECTED' => HostCardEmulationStatus.aidSelected,
    'INVALID_AID' => HostCardEmulationStatus.invalidAid,
    'PIN_VERIFIED' => HostCardEmulationStatus.pinVerified,
    'WRONG_PIN' => HostCardEmulationStatus.wrongPin,
    'FUNCTION_NOT_SUPPORTED' => HostCardEmulationStatus.functionNotSupported,
    'TAG_DISCONNECTED' => HostCardEmulationStatus.tagDisconnected,
    _ =>
      throw ArgumentError.value(
        status,
        'status',
        'No enum value with that status.',
      ),
  };
}
