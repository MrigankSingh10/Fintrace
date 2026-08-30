# Personal Finance & Budget Tracker (Native Android) - Implementation Plan

An offline-first, native Android budgeting application built in Kotlin with Jetpack Compose and Room DB. It features automated SMS transaction parsing, split-expense tracking (with separate original vs personal share calculations), user-manageable categories and payment modes, monthly salary budget tracking, and pivot-style category breakdown charts.

---

## Architectural Overview & Tech Stack

- **Platform:** Native Android (Kotlin)
- **UI Framework:** Jetpack Compose (Material 3) with modern UI/UX design (dark/light themes, card layouts, clean typography)
- **Architecture:** MVVM + Clean Architecture (Presentation, Domain/Repository, Local Data Source)
- **Database:** Room Database (SQLite) with Coroutines and Kotlin `Flow`
- **SMS Integration:** Android Telephony SMS Receiver (`RECEIVE_SMS`, `READ_SMS`) + Extensible Regex Parser Engine + Pending Review Queue
- **Analytics/Charts:** Native Jetpack Compose Canvas-based Donut/Pie Charts & Pivot Summary Tables

---

## Data Model Design

```
+----------------------------------------------------+
| Category                                           |
| - id: Long (PK)                                    |
| - name: String (Needs, Wants, Investments, etc.)   |
| - colorHex: String                                 |
| - iconName: String                                 |
| - isDefault: Boolean                               |
+----------------------------------------------------+
                          | 1
                          | *
+----------------------------------------------------+
| Transaction                                        |
| - id: Long (PK)                                    |
| - description: String (Merchant / Note)            |
| - timestamp: Long (Epoch millis)                   |
| - originalAmount: Double (Total charged, e.g. 200) |
| - myShareAmount: Double (User's share, e.g. 100)   |
| - categoryId: Long (FK -> Category)                |
| - paymentModeId: Long (FK -> PaymentMode)          |
| - type: TransactionType (EXPENSE, INCOME, TRANSFER)|
| - smsRawBody: String? (If parsed from SMS)         |
| - smsSender: String?                               |
| - status: TransactionStatus (CONFIRMED, PENDING)   |
| - notes: String?                                   |
+----------------------------------------------------+
       | 1                           | 1
       | *                           | *
+----------------------+     +-----------------------------------+
| TransactionSplit     |     | PaymentMode                       |
| - id: Long (PK)      |     | - id: Long (PK)                   |
| - transactionId (FK) |     | - name: String (e.g. HDFC Credit, |
| - personName: String |     |                 ICICI Coral, APAY)|
| - shareAmount: Double|     | - type: ModeType (BANK_DEBIT,     |
| - isUser: Boolean    |     |         CREDIT_CARD, UPI, CASH)   |
+----------------------+     +-----------------------------------+

+----------------------------------------------------+
| MonthlyBudgetSalary                                |
| - monthYear: String (PK, e.g. "2026-08")           |
| - salaryAmount: Double                             |
| - notes: String?                                   |
+----------------------------------------------------+
```

> [!IMPORTANT]
> **Split Calculation Principle**: All monthly totals, remaining balance calculations, and reporting pivot charts strictly use `myShareAmount`. The `originalAmount` is retained and always displayed alongside `myShareAmount` in all transaction views (e.g. "₹200 total • ₹100 my share").

---

## Phased Development Roadmap

We will build the application iteratively across 6 focused phases. Each phase will be reviewed and verified before proceeding to the next.

### Phase 1: Project Setup, Architecture & Core Data Layer (Room DB)
- Set up Android project files, Gradle build scripts with Compose BOM, Material 3, Room, Navigation, Coroutines.
- Implement Room Entities: `CategoryEntity`, `PaymentModeEntity`, `TransactionEntity`, `TransactionSplitEntity`, `MonthlyBudgetSalaryEntity`.
- Create DAOs (`CategoryDao`, `PaymentModeDao`, `TransactionDao`, `MonthlyBudgetDao`) with Reactive `Flow` queries.
- Build Database Migrations and initial Database Seeder (prepopulating default categories: *Needs*, *Wants*, *Investments*, *Hospital*, *Others*; and default payment modes: *Bank Debit*, *Credit Cards*, *UPI*).
- Implement Repositories and Unit Tests verifying CRUD operations and split mathematical calculations.

### Phase 2: Design System, Navigation & Category / Payment Mode Management
- Define Material 3 Theme (Colors, Typography, Shapes, Glassmorphism/Card aesthetics).
- Implement Core Scaffold with Navigation (Dashboard, Transactions, SMS Inbox, Analytics, Settings).
- Build **Category Management Screen**:
  - Add, edit, delete custom categories with color/icon picker.
- Build **Payment Mode Management Screen**:
  - Add and manage custom payment modes (Debit cards, specific named credit cards like *ICICI Coral*, *HDFC Millennia*, UPI apps like *APAY*, *GPay*).

### Phase 3: Transaction Engine & Split Expense UI
- Build **Add / Edit Transaction Screen**:
  - Form fields: Description, Date/Time, Payment Mode selector, Category selector.
  - Interactive **Split Expense Module**:
    - Mode toggle: *Solo (100% my share)* vs *Split with others*.
    - Input full original amount (e.g. ₹200).
    - My share input (e.g. ₹100) or quick split buttons (Split Equally by N people).
    - Participant list (Name + Share amount) for reference.
- Build **Transaction List Screen**:
  - Grouped by date (Today, Yesterday, Month).
  - Clear dual-amount badge: `₹200 (My Share: ₹100)`.
  - Search, Filter by Category, Payment Mode, and Date Range.
  - Transaction Detail Sheet / View.

### Phase 4: Monthly Salary & Budget Dashboard
- Build **Salary / Monthly Income Credited Setup**:
  - Simple modal or inline card to record the monthly credited salary (e.g. for "August 2026").
- Build **Dashboard Overview Screen**:
  - **Financial Health Summary Card**:
    - Salary Credited (`₹X`)
    - Total Spent this month (`₹Y`, computed using `myShareAmount`)
    - Remaining Balance (`₹(X - Y)`)
    - Daily spending pace / burn rate indicator
  - Quick action buttons (Add Expense, Review SMS).
  - Category mini-breakdown and Recent Transactions feed.

### Phase 5: SMS Parsing Engine & Permission Workflow
- Implement **Permission Flow**:
  - Clear, user-friendly in-app modal explaining why `RECEIVE_SMS` and `READ_SMS` are required (local-only, zero network transmission, financial privacy guarantee).
  - Graceful degradation if permissions are denied (app remains 100% functional for manual entry).
- Implement **SMS Regex Parser Engine**:
  - Parser for Indian banking SMS patterns (HDFC, ICICI, SBI, Axis, Kotak, APAY, UPI, Paytm, Cred, etc.).
  - Extracts: Merchant/Description, Amount, Date/Time, Payment Mode (Card last 4 digits, UPI, Debit).
- Build **SMS BroadcastReceiver**:
  - Automatically captures incoming transaction alerts in background and stores them as `PENDING` status.
- Build **Pending SMS Review Queue UI**:
  - Visual notification / badge on SMS tab.
  - One-tap categorization, quick split adjustment, and confirmation into confirmed transactions.
  - Option to scan existing SMS inbox for past transactions.

### Phase 6: Reporting, Pivot Summary & Visual Analytics
- Implement **Category Breakdown Donut / Pie Chart** in Jetpack Compose:
  - Displays total spending per category and percentage of total spend for the selected period.
  - Interactive segment highlighting on touch.
- Implement **Pivot Summary Table**:
  - Columns: `Category` | `Total Amount (My Share)` | `% of Total Spend`
  - Matches the user's Excel pivot structure exactly.
- Add Period Selector: Current Month, Past Months, Custom Date Range.
- Export Data feature (Export transactions to CSV for Excel backup).

---

## Verification Plan

### Automated Verification
- Unit tests for `TransactionDao` and `MonthlyBudgetDao` calculations (verifying `myShareAmount` sum vs `originalAmount`).
- Unit tests for the `SmsParser` regex engine against a suite of sample Indian bank SMS messages.

### Manual Verification
- Testing CRUD flows for Categories and Payment Modes.
- Testing Split Expense creation and verifying that Dashboard remaining balance updates based on user share.
- Testing SMS parser on sample SMS payloads and verifying the pending confirmation queue.
- Checking Donut Chart and Pivot Table rendering with various transaction amounts.
