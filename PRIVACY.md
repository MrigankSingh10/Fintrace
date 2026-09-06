# Privacy and data handling

This describes the repository's current implementation, not a hosted service.

## Stored information

Fintrace stores transactions, categories, payment modes, split participants, salary settings, and transaction status in a Room database in Android app storage. Imported transactions retain the SMS body, sender, and timestamp. Theme choice is stored in preferences.

Dismissed messages remain stored so rescans recognize them. Restore returns them to Pending Review. Neither action changes the original SMS in the device's messaging app.

## Permissions and connectivity

- `READ_SMS` supports scanning inbox messages; the review screen currently requests a 30-day lookback.
- `RECEIVE_SMS` supports parsing new incoming SMS broadcasts.

The manifest does not declare Internet permission. The repository has no app backend, advertising SDK, or analytics service. No bank credentials are needed.

## Backup and export

Android backup is enabled. Backup and device-transfer rules explicitly include `finance_tracker_db`. Whether and where a backup occurs depends on Android and device/account settings; do not assume this database can never leave the device.

Excel reports are written through Android's file picker. The selected destination may be local storage or a cloud document provider. The current analytics export contains confirmed expenses; it is not a full, restorable database backup.

Fintrace does not implement its own database encryption or app lock. Android app-storage protection is distinct from these features.

## Sharing and reporting

Use fictional data in screenshots, public issues, and tests. Remove financial information from logs and attachments. Report vulnerabilities privately through [SECURITY.md](SECURITY.md).
