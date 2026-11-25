/// Application identifier (AID) of the emulated demo card.
///
/// Must match the `aid-filter` in
/// `android/app/src/main/res/xml/apduservice.xml`, otherwise Android does not
/// route the reader's SELECT command to the emulating device.
const demoAid = 'F000000001';

/// Sample PIN shared by the emulating device and the reader, sent as UTF-8.
///
/// Not a secret. In a real app, take the PIN from the user, not from code.
const demoPin = '1234';
