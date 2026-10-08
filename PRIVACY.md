# Fintrace Privacy Policy

**Effective date: October 8, 2026**

Fintrace is an offline personal-finance Android application maintained by **Mrigank Singh** as the **Fintrace project**. This policy describes data handled by the app installed on your device. It does not describe a hosted service; the developer does not operate a Fintrace backend.

## Data the app accesses and why

If you grant SMS permissions, Fintrace reads the sender/address, message body, and message timestamp from SMS messages. When you choose to scan your inbox, Fintrace scans messages from the preceding 30 days. While the app is closed, Android can deliver newly received SMS messages to Fintrace for parsing. These features support detecting possible financial transaction alerts and creating records for your review. The parser may match financial transaction messages from different senders; it does not require that a sender be a bank.

For messages that match, Fintrace stores the original SMS body, sender, and source timestamp with a locally stored transaction. It also stores parsed financial details such as description, amount, date, currency, transaction type, payment mode, and parse confidence. Messages that do not match are not added as transaction records by the parser. Fintrace does not modify or delete the source SMS in your messaging app.

You can use Fintrace without SMS access. Manual transaction entry remains available if you deny or later revoke SMS permissions.

## Information stored on the device

Fintrace stores information you enter or that it derives for your finance records in the app's local Room database. This can include transactions and their status (including Pending Review and Dismissed), categories, payment modes and card mappings, split participants and shares, recurring transaction details, monthly income or budget settings, and SMS-derived details described above. Theme preference is stored locally. Fintrace does not require an account and does not collect bank login credentials.

## Collection, sharing, and transfer

The app declares no Internet permission and has no developer-operated backend, advertising SDK, or analytics service. The developer does not receive or collect the SMS or financial information described in this policy through the app, and Fintrace does not send or share that information with the developer or another service.

Android backup and device-transfer behavior can copy app data depending on the Android version, device configuration, and the backup or transfer service you use. Fintrace's backup configuration includes its finance database; therefore, do not assume stored information can never leave the device. If you export a report, you choose its destination through Android's file picker. The destination may be local storage or a cloud document provider, which may handle the exported file under its own terms and privacy policy. Exports are initiated by you.

## Security

Fintrace keeps its database in Android app-specific storage and relies on Android's app sandbox and device security. Fintrace does not implement its own database encryption or app lock, and this policy does not claim that stored data is encrypted by Fintrace. Protect your device and review its screen-lock, backup, and device-transfer settings.

## Retention and deletion

Records remain in the app database until you delete them in the app where deletion is available, clear Fintrace's app data, or uninstall Fintrace. Dismissed SMS-derived records remain in the database so inbox rescans can recognize them and avoid reimporting them; you can manage these records in the app. Clearing app data or uninstalling removes Fintrace's local app data, but does not delete SMS messages from your messaging app, exported files you saved elsewhere, or copies retained by an Android backup or a transfer service. Manage those copies with the relevant Android or file-provider controls.

## Audience

Fintrace is a general-purpose personal-finance utility. It is not designed specifically for children and does not require an account or ask for a user's age. Use of the app by children should be guided by a parent or guardian and comply with applicable local requirements.

## Questions and security reports

For privacy questions or requests, use the public [Fintrace GitHub repository Issues](https://github.com/MrigankSingh10/Fintrace/issues). Do not post SMS bodies, account details, or other financial information in a public issue. For potential security vulnerabilities, follow the private reporting instructions in [SECURITY.md](https://github.com/MrigankSingh10/Fintrace/blob/main/SECURITY.md); do not disclose vulnerability details in a public issue.

This policy applies to the Fintrace Android app and may be updated when app behavior changes. The current version is published in the [Fintrace repository](https://github.com/MrigankSingh10/Fintrace/blob/main/PRIVACY.md).
