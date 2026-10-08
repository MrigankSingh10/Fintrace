# Google Play release checklist

This checklist describes the Play Console steps for Fintrace. Keep declarations
aligned with the behavior of the version being uploaded and re-check current
Google Play policy before every release.

## Build and signing

- Build a signed Android App Bundle (`.aab`) for the `release` variant.
- Use version code `7` and version name `1.3.1` for the first Play-ready build.
- Keep the keystore and passwords outside Git. Back up the keystore securely.
- Enroll in Play App Signing. To allow an installation from the GitHub APK to be
  updated by Google Play, choose the existing Fintrace app-signing key during
  enrollment rather than allowing Play to generate a different app-signing key.
- Verify the uploaded bundle reports package `com.fintrace.app`, target API 36,
  and the intended version before promoting it beyond internal testing.

Official signing guidance:
<https://developer.android.com/studio/publish/app-signing>

## Store setup

- App type: App
- Category: Finance
- Pricing: Free, unless the product model changes
- App access: No login or restricted account is required
- Ads: No, while Fintrace contains no advertising SDK or advertising content
- Privacy policy URL:
  <https://github.com/MrigankSingh10/Fintrace/blob/main/PRIVACY.md>
- Complete the content-rating and target-audience questionnaires truthfully.
  Do not select child age groups unless the app and listing are intentionally
  designed to meet the Families requirements.
- Supply a monitored support email in the Store settings. Do not use a private
  credential or an email address that cannot receive Play review questions.

## Restricted SMS permissions

Fintrace requests `READ_SMS` and `RECEIVE_SMS` for the permitted
**SMS-based money management** use case. Complete the Permissions Declaration
Form after uploading the bundle.

The declaration and review video should demonstrate:

1. The SMS review feature is prominent in the app and store description.
2. Fintrace shows its in-app disclosure immediately before Android's permission
   dialog and requires the user to tap **Allow SMS**.
3. **Not now** leaves manual transaction entry fully usable.
4. `READ_SMS` scans up to 30 days of existing messages only when the user starts
   a scan.
5. `RECEIVE_SMS` processes new incoming transaction alerts while the app is not
   open and places matches into Pending Review.
6. Sender, body, timestamp, and parsed financial details for matches are stored
   locally for review and duplicate prevention.
7. The app has no Internet permission, backend, advertising SDK, or analytics
   SDK. Android backup/device transfer and user-selected export destinations are
   disclosed separately.

Official SMS permission policy:
<https://support.google.com/googleplay/android-developer/answer/10208820>

## App-content declarations

- **Financial features:** complete the declaration. Fintrace is a personal
  money-management and budgeting utility; it does not provide banking, loans,
  payments, money transfer, trading, insurance, credit scoring, or personalized
  financial advice. Select the option that accurately represents this behavior
  in the current Console form (typically **Other** when personal money
  management is requested), and describe the offline budgeting use case.
- **Data safety:** complete the form even though Fintrace has no developer
  backend. Under Google's definition, data processed only on-device is not
  collected by the developer. Confirm the answers against the current build,
  Android backup behavior, and user-initiated exports; keep them consistent
  with `PRIVACY.md` and do not claim app-level encryption.
- **Privacy policy:** confirm the public URL opens without authentication and is
  also reachable from Settings inside the app.
- **Account deletion:** Fintrace has no account creation or remote account data.
- **App access:** no reviewer credentials are required.

Official declaration guidance:

- <https://support.google.com/googleplay/android-developer/answer/10787469>
- <https://support.google.com/googleplay/android-developer/answer/13849271>

## Listing assets

- 512 x 512 app icon (32-bit PNG)
- 1024 x 500 feature graphic (JPEG or 24-bit PNG without alpha)
- At least two accurate phone screenshots; four portrait screenshots at
  1080 x 1920 or higher are recommended
- Short description (80 characters maximum)
- Full description (4,000 characters maximum)
- Unique screenshot alt text and no real SMS or financial information

Official asset requirements:
<https://support.google.com/googleplay/android-developer/answer/9866151>

## Test and rollout

1. Upload the signed AAB to Internal testing first.
2. Review Play's automated pre-launch report and policy alerts.
3. Test install/upgrade, SMS denial and grant, inbox scan, incoming SMS, manual
   entry, recurring transactions, split bills, export, light/dark themes, and
   database retention on representative devices.
4. If the developer account is a personal account created after November 13,
   2023, run a closed test with at least 12 continuously opted-in testers for 14
   days before applying for production access.
5. Submit the production release only after the SMS declaration and all App
   content sections are complete. Use managed publishing if release timing must
   remain under manual control after review.

Official testing requirements:
<https://support.google.com/googleplay/android-developer/answer/14151465>

## Current API requirement

From August 31, 2026, new phone/tablet apps and updates must target Android 16
(API level 36) or higher. Fintrace v1.3.1 targets API 36 while retaining
`minSdk` 26.

Official target API requirements:
<https://support.google.com/googleplay/android-developer/answer/11926878>
