# Architecture and engineering notes

Fintrace is a single-module Android app using MVVM presentation and a repository over Room. The application class creates dependencies; a ViewModel factory supplies them to screens.

## Code map

```text
app/src/main/java/com/fintrace/app/
├── MainActivity.kt          App shell and theme toggle
├── FinanceTrackerApp.kt    Dependency creation and display preferences
├── data/
│   ├── local/             Room entities, DAOs, relations, converters, seeding
│   ├── model/             Transaction/status/payment-mode enums
│   ├── repository/        Persistence and financial summary operations
│   └── sms/               Parser, receiver, and inbox scanner
├── ui/
│   ├── navigation/        Routes and ViewModel factory
│   ├── dashboard/         Monthly totals and spending pace
│   ├── transactions/      Monthly history, filters, editing, and splits
│   ├── sms/               Pending and dismissed review
│   ├── analytics/         Charts, tables, export orchestration
│   ├── categories/        Category management
│   ├── paymentmodes/      Payment-mode management
│   ├── components/        Shared display components
│   └── theme/             Material colors, typography, shapes
└── util/                  XLSX generation
```

## Data flow

Screens observe ViewModel state. ViewModels derive `StateFlow` values from repository flows. Room queries emit updates after table changes. The SMS receiver and scanner also write imports through the DAO.

Imported SMS starts as `PENDING`. Confirmation makes it eligible for financial queries. Dismissal stores `DISMISSED`; restoration returns it to `PENDING`. Duplicate lookup matches raw SMS body across all statuses.

## Accounting model

Transactions reference categories and payment modes and can have multiple split rows. Both `original_amount` and `my_share_amount` are retained. Saving a transaction with splits is a Room transaction.

Expense queries use personal share for spending and full amount for original charges. A positive confirmed-income total takes precedence over manual salary for the month. If that total falls to zero, manual salary becomes the fallback again.

Income hides category/payment-mode badges, but non-null foreign keys still require internal references. The schema has not yet separated income from those relationships.

Month navigation uses local-time boundaries through the last millisecond before the next month. Analytics has separate timeframe calculations.

## Engineering choices

- On-device parsing and storage avoid a backend or bank authentication dependency.
- Review status gives users control before imports affect totals.
- Separate original/personal amounts support split reporting.
- Compose Canvas and the custom XLSX writer avoid dedicated chart/spreadsheet runtime dependencies.
- Stored dismissals distinguish ignored messages from unseen ones during rescans.

## Known limitations and next steps

These are improvement opportunities, not release commitments.

- **Parser ambiguity:** Keyword rules do not cover every bank format. Messages containing both debit and credit keywords currently favor expense classification. Card names include specific default mappings.
- **Duplicate identity:** Body-only matching can collapse legitimate identical messages. Concurrent receiver/scanner imports lack a unique SMS identity constraint.
- **Confirmation paths:** SMS imports require review; saving the manual/edit form confirms directly. Income editing still needs refinement, including split controls.
- **Income accounting:** Refund/cashback patterns may become income. Income totals sum personal-share amounts; historical split income requires care. Manual salary returns when confirmed income falls to zero.
- **Percentages:** In-app category values use salary, while donut geometry and Excel percentages use spending. Multi-month analytics mixes range income with a first-month manual fallback. These rules need an explicit product decision before changes.
- **Database upgrades:** Room is version 1 with schema export disabled and destructive migration fallback configured. Future schema changes need explicit, tested migrations before distribution.
- **Coverage:** Tests cover parser examples, basic arithmetic, month ranges, and status conversion. Database reopen/migration, dismiss/restore integration, and Compose UI tests remain to be added.
- **Development isolation:** Debug and release share the application ID. A debug suffix is documented but not configured.
- **Privacy:** Backup includes the database; there is no app-level database encryption. See [PRIVACY.md](../PRIVACY.md).

The original [implementation plan](../implementation_plan.md) is historical planning material, not a list of shipped features.
