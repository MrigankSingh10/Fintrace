# Contributing to Fintrace

Read the [README](README.md), [architecture notes](docs/ARCHITECTURE.md), and [testing guide](TESTING_GUIDE.md). Discuss substantial features or data-model changes in an issue first. Small bug fixes can go directly to a pull request. Security reports belong at the private contact in [SECURITY.md](SECURITY.md).

## Development workflow

1. Fork the repository and create a focused branch.
2. Configure JDK 17 and Android SDK Platform 34.
3. Reproduce the problem with synthetic data on a dedicated emulator.
4. Implement the change and add relevant regression coverage.
5. Run `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
6. Open a pull request explaining the problem, new behavior, and verification.

For UI changes, include screenshots with fictional data and check both themes. State which checks were not run; a passing unit suite is not device verification.

## Code and data conventions

- Follow existing Kotlin/Compose conventions and repository/DAO boundaries.
- Keep transaction direction separate from payment mode.
- Preserve full-bill versus personal-share amounts.
- Exclude pending and dismissed records from financial totals.
- Check month boundaries and device time zones when changing date filtering.
- Discuss changes to salary denominators explicitly.
- Schema changes need tested migrations that preserve existing records; a reset is not an upgrade solution.

## SMS parser contributions

Provide a synthetic message retaining the relevant wording and punctuation. Replace names, account/card digits, references, amounts, balances, phone numbers, and dates. State the expected amount, direction, merchant, and payment mode where applicable.

Add a regression to `SmsParserTest.kt`. Include counterexamples where useful, such as a debit alert saying the recipient was credited.

Never commit real SMS, databases, financial exports, credentials, signing keys, or machine-specific SDK paths.

## Collaboration

Keep feedback respectful, specific, and focused on the work. There is no guaranteed review or release turnaround. Contributions use the repository's existing [license](LICENSE).
