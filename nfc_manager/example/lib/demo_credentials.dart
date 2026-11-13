/// Application ID of the demo applet.
///
/// Must match the `aid-filter` in
/// `android/app/src/main/res/xml/apduservice.xml`, otherwise Android does not
/// route the reader's SELECT command to the emulating device.
const demoAid = 'F000000001';

/// Sample PIN for this demo, shared by the emulating device and the reader.
/// Sent as UTF-8 bytes.
const demoPin = '1234';
