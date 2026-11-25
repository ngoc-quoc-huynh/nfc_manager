# nfc_manager_example

Demonstrates `nfc_manager` on Android: read an ISO-DEP tag, or let one phone
emulate a card that a second phone reads.

Run it with `flutter run` on a phone with NFC turned on.

- **Read a tag.** Open **Tag Reader**, tap **Start discovery**, and hold an
  ISO-DEP tag near the phone. The app shows the tag ID.
- **Emulate a card.** On phone A, open **Host Card Emulation** and tap
  **Start emulation**. Keep phone A unlocked. On phone B, start discovery in
  **Tag Reader** and hold the phones back to back. Then tap **Select AID**,
  then **Verify PIN**. Both answer with status `9000`.

Both phones use the demo AID and PIN from [lib/demo_card.dart](lib/demo_card.dart).
The AID must match `android/app/src/main/res/xml/apduservice.xml`.
