enum HostCardEmulationStatus {
  ready,
  invalidCommand,
  invalidLcLength,
  aidSelected,
  invalidAid,
  pinVerified,
  wrongPin,
  functionNotSupported,
  tagDisconnected;

  factory HostCardEmulationStatus.fromString(String status) => switch (status) {
        'READY' => HostCardEmulationStatus.ready,
        'INVALID_COMMAND' => HostCardEmulationStatus.invalidCommand,
        'INVALID_LC_LENGTH' => HostCardEmulationStatus.invalidLcLength,
        'AID_SELECTED' => HostCardEmulationStatus.aidSelected,
        'INVALID_AID' => HostCardEmulationStatus.invalidAid,
        'PIN_VERIFIED' => HostCardEmulationStatus.pinVerified,
        'WRONG_PIN' => HostCardEmulationStatus.wrongPin,
        'FUNCTION_NOT_SUPPORTED' =>
          HostCardEmulationStatus.functionNotSupported,
        'TAG_DISCONNECTED' => HostCardEmulationStatus.tagDisconnected,
        _ => throw ArgumentError.value(
            status,
            'status',
            'No enum value with that status.',
          ),
      };
}
