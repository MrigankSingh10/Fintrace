# Architecture and engineering notes

Fintrace is a single-module Android app using MVVM presentation and a repository over Room. The application class creates dependencies; a ViewModel factory supplies them to screens.

## Code map

```text
app/src/main/java/com/fintrace/app/
├── MainActivity.kt          Edge-to-edge app shell, startup SMS disclosure, and theme toggle
├── FinanceTrackerApp.kt    Dependency creation, preferences, and one-time data backfills
├── data/
│   ├── local/             Room entities, DAOs, relations, converters, seeding
│   ├── model/             Transaction/status/payment-mode enums
│   ├── repository/        Persistence and financial summary operations
│   └── sms/               Parser, receiver, and inbox scanner
├── ui/
│   ├── navigation/        Routes and ViewModel factory
│   ├── dashboard/         Monthly totals, category budgets, and review status
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

Imported SMS starts as `PENDING`. Confirmation makes it eligible for financial queries. Dismissal stores `DISMISSED`; restoration returns it to `PENDING`. Duplicate lookup uses the raw body, sender, and source timestamp across all statuses, with a timestamp fallback for rows created before source timestamps were stored. The lookup and insert run in one Room transaction.

## Accounting model

Transactions reference categories and payment modes and can have multiple split rows. Both `original_amount` and `my_share_amount` are retained. Saving a transaction with splits is a Room transaction.

Expense queries use personal share for spending and full amount for original charges. Monthly income is `confirmed income + manual adjustment`: all confirmed income transactions count regardless of wording, and the manual adjustment stored in `monthly_budgets.salary_amount` is added on top for the whole month. Because the adjustment is persistent rather than a frozen total, later income keeps accumulating, and dismissing an income transaction lowers the month without touching the adjustment.

Income hides category/payment-mode badges, but non-null foreign keys still require internal references. The schema has not yet separated income from those relationships.

Month navigation uses local-time boundaries through the last millisecond before the next month. Analytics has separate timeframe calculations.

Categories can store either a fixed budget amount or a percentage of income. Percentage budgets are derived month by month from canonical income; a month with nonpositive income contributes zero to the percentage-budget base. Multi-month analytics sums the confirmed income and manual adjustment for every intersecting month.

Recurring transactions use a series identifier, anchor day, and `yyyy-MM` occurrence key. The repository creates missing monthly occurrences without duplicating an existing series/month pair. Transaction history presents recurring rows first and groups other rows by local calendar day.

## Engineering choices

- On-device parsing and storage avoid a backend or bank authentication dependency.
- Review status gives users control before imports affect totals.
- Separate original/personal amounts support split reporting.
- Compose Canvas and the custom XLSX writer avoid dedicated chart/spreadsheet runtime dependencies.
- Stored dismissals distinguish ignored messages from unseen ones during rescans.
- Explicit Room migrations preserve released data through schema version 8.
- Release builds use R8 optimization and resource shrinking; the UI uses Android's edge-to-edge APIs and handles system-bar insets.

## Known limitations and next steps

These are improvement opportunities, not release commitments.

- **Parser coverage:** Pattern rules do not cover every bank and card format. Card names include specific default mappings, and unfamiliar messages may still require manual entry or parser improvements.
- **Duplicate identity:** SMS identity is enforced transactionally by body, sender, and source timestamp, but it is not backed by a dedicated database uniqueness constraint. Legacy rows use a timestamp fallback.
- **Confirmation paths:** SMS imports require review; saving the manual/edit form confirms directly. Income editing still needs refinement, including split controls.
- **Income accounting:** Refund/cashback patterns may become income, and every confirmed income counts whether it arrived by SMS or by hand. Income totals sum personal-share amounts; historical split income requires care. There is no way to exclude one confirmed income from the month other than dismissing it.
- **Percentages:** Category budget percentages use income, while donut geometry and Excel category percentages describe shares of spending. The two labels intentionally have different denominators and should remain explicit in UI and documentation.
- **Database upgrades:** Room is version 8 with explicit migrations from versions 1 through 8. Schema export remains disabled, so future schema changes need explicit migrations and stronger migration/reopen coverage before distribution.
- **Coverage:** Unit tests cover parser examples, SMS identity, income and category-budget rules, recurrence, month ranges, amount formatting, and presentation logic. Database reopen/migration, dismiss/restore integration, and Compose UI tests remain to be added.
- **Development isolation:** Debug and release share the application ID. A debug suffix is documented but not configured.
- **Privacy:** Backup includes the database; there is no app-level database encryption. See [PRIVACY.md](../PRIVACY.md).

The original [implementation plan](../implementation_plan.md) is historical planning material, not a list of shipped features.

## Package and release identity

Fintrace v1.4.0, current debug builds, and the Google Play build use application ID `com.mrigank.fintrace`. GitHub releases v1.3.0 and earlier used `com.fintrace.app`. Android treats these as different applications with separate sandboxes and Room databases; installing v1.4.0 does not upgrade or automatically copy data from an older-package installation.
