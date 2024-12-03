import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/src/tag.dart';

void main() {
  test(
    'call returns id.',
    () => expect(Tag('id').call(), 'id'),
  );
}
