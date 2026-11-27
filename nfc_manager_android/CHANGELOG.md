## 2.0.0


- Require Dart `^3.12.0` and Flutter `>=3.44.0`.
- HCE returns `67 00` for short headers, `6D 00` for unknown instructions, and `69 85` for PIN
  verification before AID selection.
- HCE returns `63 CX` for a wrong PIN, with `X` attempts left. The third wrong attempt returns
  `69 83` and ends the session.
- Validate HCE arguments, compare PINs in constant time, and clean up HCE sessions on detach.
- Reject short APDUs with `Lc = 00` and keep the tag connected after oversized commands.
- Serialize tag I/O and stop discovery when the activity detaches or the reader is disposed.
- Report native error codes and fix tag ID formatting on older Kotlin hosts.

## 1.2.0

- Set Dart constraint to `^3.0.0`.
- Set Flutter constraint to `^3.41.0`.

## 1.1.0

- Update dependencies.
- Update Dart to `3.8.0`.

## 1.0.1

- Handle SecurityException on isoDep.close ().

## 1.0.0

- Initial release.
