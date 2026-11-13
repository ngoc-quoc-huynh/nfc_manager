# nfc_manager_example

Demonstrates how to use the nfc_manager plugin: reading tags and
emulating a card with Host Card Emulation (HCE).

## Prerequisites

- A physical device with NFC. Emulators and simulators have no NFC
  hardware.
- For the Host Card Emulation page: an Android device with HCE support.
- For the Tag Reader page: a second device or an NFC tag to read. To try
  the APDU commands (Select AID, Verify PIN), use a second device running
  the Host Card Emulation page of this app as the card, and hold the two
  devices back to back.
- The AID and PIN must match on both sides. They are defined once in
  `lib/demo_credentials.dart`, and the AID must also match the
  `aid-filter` in `android/app/src/main/res/xml/apduservice.xml`. If you
  change one, change the others.
