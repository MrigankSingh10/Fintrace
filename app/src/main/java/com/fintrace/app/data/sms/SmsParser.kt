package com.fintrace.app.data.sms

import com.fintrace.app.data.model.ParseConfidence
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.TransactionType
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsTransaction(
    val amount: Double,
    val merchant: String,
    val paymentModeType: PaymentModeType? = null,
    val paymentModeName: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val rawBody: String,
    val sender: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val parseConfidence: ParseConfidence = ParseConfidence.FULL,
    val cardLastFour: String? = null,
    val currencyCode: String = "INR"
)

object SmsParser {

    // Regex for transaction amounts: INR, Rs, ₹, USD, $, EUR, €, GBP, £, AED, SGD, CAD, AUD, JPY, ¥, etc.
    private val AMOUNT_PATTERN = Pattern.compile(
        "((?:INR|RS\\.?|₹|USD|\\$|EUR|€|GBP|£|AED|SGD|CAD|AUD|JPY|¥))\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    private val CURRENCY_TO_CODE = mapOf(
        "INR" to "INR",
        "RS" to "INR",
        "₹" to "INR",
        "GBP" to "GBP",
        "£" to "GBP",
        "USD" to "USD",
        "$" to "USD",
        "EUR" to "EUR",
        "€" to "EUR",
        "AED" to "AED",
        "SGD" to "SGD",
        "CAD" to "CAD",
        "AUD" to "AUD",
        "JPY" to "JPY",
        "¥" to "JPY"
    )

    fun normalizeCurrencyToken(raw: String): String =
        CURRENCY_TO_CODE[raw.trim().replace(".", "").uppercase()] ?: "INR"

    // Strong evidence that money actually moved. Word boundaries prevent accidental
    // matches such as "prepaid", while the directed-payment patterns avoid treating
    // promotional uses of "pay" as completed transactions.
    private val DEBIT_ACTION_PATTERNS = listOf(
        Pattern.compile("\\bdebited\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bspent\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bcharged\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bwithdrawn\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdeducted\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:paid|transferred|sent)\\s+to\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile(
            "\\b(?:paid|transferred|sent)\\b\\s+(?:(?:INR|RS\\.?|₹|USD|\\$|EUR|€|GBP|£|AED|SGD|CAD|AUD|JPY|¥)\\s*)?[0-9][0-9,]*(?:\\.[0-9]{1,2})?\\s+to\\b",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile("\\b(?:txn|transaction)\\s+(?:of|at)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpurchase\\s+of\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bused\\s+(?:at|on)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bswiped(?:\\s+(?:at|on))?\\b", Pattern.CASE_INSENSITIVE)
    )

    private val CREDIT_ACTION_PATTERNS = listOf(
        Pattern.compile("\\bcredited\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdeposited\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\breceived\\s+from\\b", Pattern.CASE_INSENSITIVE)
    )

    // Standalone refund/cashback/salary wording is common in advertising. Refund and
    // cashback messages without a normal credit verb are accepted only when they say
    // that the event itself has completed.
    private val COMPLETED_CREDIT_PATTERN = Pattern.compile(
        "\\b(?:refund|cashback)\\b.{0,80}\\b(?:processed|completed)\\b",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    private val TRANSACTION_TERM_PATTERN = Pattern.compile("\\b(?:txn|transaction)\\b", Pattern.CASE_INSENSITIVE)
    private val COMPLETION_PATTERN = Pattern.compile(
        "\\b(?:successful|successfully|completed|done|approved)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val NON_POSTED_PATTERN = Pattern.compile(
        "\\b(?:failed|declined|cancelled|canceled|unsuccessful|reversed|reversal)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // Senders typically associated with banks / finance alerts
    private val BANK_SENDER_KEYWORDS = listOf(
        "BANK", "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "PNB", "BOB", "CANARA", "UNION",
        "INDUS", "YESBK", "RBL", "FED", "IDBI", "IDFC", "PAYTM", "GPAY", "AMAZON", "APAY",
        "CITI", "SCB", "STANDARD", "BAROD", "CRED", "SLICE", "JUPITER", "FI", "BOBCARD", "SCAPIA"
    )

    // OTP / Non-transaction filters to strictly ignore
    private val IGNORE_KEYWORDS = listOf(
        "otp", "one time password", "verification code", "secret code", "login", "password reset"
    )

    // Keywords indicating credit card bill payment acknowledgment (to ignore as expense)
    private val CARD_BILL_PAYMENT_PATTERNS = listOf(
        Pattern.compile("payment.*received towards.*(?:credit)?\\s*card", Pattern.CASE_INSENSITIVE),
        Pattern.compile("received towards your (?:credit)?\\s*card", Pattern.CASE_INSENSITIVE),
        Pattern.compile("cardmember,\\s*payment of.*received", Pattern.CASE_INSENSITIVE),
        Pattern.compile("autopay.*payment received for.*card", Pattern.CASE_INSENSITIVE),
        Pattern.compile("bill payment received for.*credit card", Pattern.CASE_INSENSITIVE),
        Pattern.compile("payment of.*received for your credit card", Pattern.CASE_INSENSITIVE)
    )

    // Balance-only / statement-due alerts (no money moved) — ignored only when
    // no debit/credit action verb is present, so real transactions that carry an
    // "Avl Lmt / Available Balance" suffix still parse.
    private val BALANCE_UPDATE_PATTERNS = listOf(
        Pattern.compile("available\\s+bal", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bavl\\s+bal", Pattern.CASE_INSENSITIVE),
        Pattern.compile("balance\\s+as\\s+on|bal\\s+as\\s+on", Pattern.CASE_INSENSITIVE),
        Pattern.compile("as\\s+on\\s+yesterday", Pattern.CASE_INSENSITIVE),
        Pattern.compile("cheques?\\s+are\\s+subject\\s+to\\s+clearing", Pattern.CASE_INSENSITIVE),
        Pattern.compile("for\\s+updated\\s+a/c\\s+bal", Pattern.CASE_INSENSITIVE),
        Pattern.compile("a/c\\s+balance", Pattern.CASE_INSENSITIVE)
    )

    private val STATEMENT_DUE_PATTERNS = listOf(
        Pattern.compile("total\\s+due\\s+for\\s+statement", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\btotal\\s+due\\b|\\bminimum\\s+due\\b|\\bmin\\s+due\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("statement\\s+(is\\s+generated|due)|payment\\s+due|due\\s+date", Pattern.CASE_INSENSITIVE)
    )

    // High-priority merchant extraction patterns (ordered from most specific to general)
    private val MERCHANT_PATTERNS = listOf(
        // "; Zepto credited." or "; Umed Singh Goud credited. UPI:..."
        Pattern.compile("(?:;|\\.|\\b)\\s*([A-Za-z0-9\\.\\-_@&/ ]+?)\\s+credited(?:\\.|;|\$|\\s+upi|\\s+ref|\\s+call)", Pattern.CASE_INSENSITIVE),
        // "at SWIGGY on 28-Aug" or "at AMAZON INDIA." or "AT HOLAFLY WAS SUCCESSFUL"
        Pattern.compile("\\bat\\s+([A-Za-z0-9\\.\\-_@&/ ]+?)(?:\\s+was|\\s+is|\\s+on|\\s+ref|\\s+avl|\\s+bal|\\s+tot|\\s+using|\\s+limit|\\.|\$|;)", Pattern.CASE_INSENSITIVE),
        // "paid to SWIGGY on" / "transfer to John on" / "sent to John"
        Pattern.compile("(?:paid|transferred|transfer|sent)\\s+to\\s+([A-Za-z0-9\\.\\-_@&/ ]+?)(?:\\s+was|\\s+is|\\s+on|\\s+ref|\\s+using|\\s+via|\\s+avl|\\s+bal|\\.|\$|;)", Pattern.CASE_INSENSITIVE),
        // "spent on SWIGGY on" / "spent at SWIGGY" / "towards SWIGGY on"
        Pattern.compile("(?:spent\\s+(?:on|at)|towards)\\s+([A-Za-z0-9\\.\\-_@&/ ]+?)(?:\\s+was|\\s+is|\\s+on|\\s+using|\\s+via|\\s+avl|\\s+bal|\\.|\$|;)", Pattern.CASE_INSENSITIVE),
        // "vpa swiggy@icici on"
        Pattern.compile("\\bvpa\\s+([A-Za-z0-9\\.\\-_@&]+)", Pattern.CASE_INSENSITIVE),
        // "to VPA swiggy@icici" or "to merchant xyz"
        Pattern.compile("\\bto\\s+(?:vpa\\s+)?([A-Za-z0-9\\.\\-_@& ]+?)(?:\\s+was|\\s+is|\\s+on|\\s+ref|\\s+avl|\\s+bal|\\s+tot|\\s+using|\\.|\$|;)", Pattern.CASE_INSENSITIVE),
        // "info/SWIGGY/1234" or "UPI/SWIGGY/123"
        Pattern.compile("(?:info|inf|desc)[\\*:/\\s]+(?:ach\\*|upi/|imps/|neft/)?([A-Za-z0-9\\.\\-_@& ]+?)(?:\\*[0-9]+|/[0-9]+|\\s+on|\\s+ref|\\s+avl|\\s+bal|\\.|\$|;)", Pattern.CASE_INSENSITIVE),
        // "UPI/123456/SWIGGY"
        Pattern.compile("(?:upi|imps|neft)/[0-9]+/([A-Za-z0-9\\.\\-_@& ]+?)(?:/|\\s+on|\\s+ref|\\s+avl|\\.|\$|;)", Pattern.CASE_INSENSITIVE)
    )

    // Regex to detect purely numeric or amount string that should NEVER be a merchant
    private val NUMERIC_OR_AMOUNT_REGEX = Regex("^(?:INR|RS\\.?|₹|USD|\\$|EUR|€|GBP|£|AED|SGD|CAD|AUD|JPY|¥)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?\\s*$", RegexOption.IGNORE_CASE)
    private val INVALID_WORDS = listOf(
        "a transaction of", "transaction of", "card ending", "account ending", "your account",
        "your card", "credit card", "debit card", "bank", "atm", "purchase of", "payment of",
        "for inr", "for rs", "by inr", "by rs", "avl lmt", "avl bal", "ref no", "upi ref"
    )

    // Card pattern (e.g. Card ending 1234, Card XX1234, AMEX card ** 81000 with 4-6 digit tail)
    private val CARD_PATTERN = Pattern.compile(
        "(?:credit\\s*card|card|ending\\s*with|ending)\\s*(?:ending|no|\\s)*[xX*·•\\-]*\\s*([0-9]{4,6})",
        Pattern.CASE_INSENSITIVE
    )

    // Bank Account pattern (e.g. Account XX857, A/C 1234, Bank Account)
    private val BANK_ACCOUNT_PATTERN = Pattern.compile(
        "(?:account|a/c|acct)\\s*[xX*]*([0-9]{3,4})",
        Pattern.CASE_INSENSITIVE
    )

    fun extractCardLastFour(body: String): String? {
        val matcher = CARD_PATTERN.matcher(body)
        return if (matcher.find()) matcher.group(1) else null
    }

    fun parse(smsBody: String, sender: String? = null, timestamp: Long = System.currentTimeMillis()): ParsedSmsTransaction? {
        val lower = smsBody.lowercase()
        val senderUpper = (sender ?: "").uppercase()

        // 1. Filter out OTPs or non-financial messages
        if (IGNORE_KEYWORDS.any { lower.contains(it) }) {
            return null
        }

        // 2. Ignore Credit Card Bill Payment receipts (e.g. "payment received towards your credit card")
        if (CARD_BILL_PAYMENT_PATTERNS.any { it.matcher(smsBody).find() }) {
            return null
        }

        // Failed, declined, cancelled, and reversed attempts did not produce a posted
        // transaction and must not enter Pending Review as spending.
        if (NON_POSTED_PATTERN.matcher(smsBody).find()) {
            return null
        }

        // 3. Extract Amount (required per specification)
        val amountMatcher = AMOUNT_PATTERN.matcher(smsBody)
        if (!amountMatcher.find()) {
            return null
        }

        val rawAmountStr = amountMatcher.group(2)?.replace(",", "") ?: return null
        val amount = rawAmountStr.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null
        val currencyCode = normalizeCurrencyToken(amountMatcher.group(1) ?: "INR")

        // 4. Check for Financial Action (Debit or Credit)
        val isDebit = DEBIT_ACTION_PATTERNS.any { it.matcher(smsBody).find() }
        val isCredit = CREDIT_ACTION_PATTERNS.any { it.matcher(smsBody).find() } ||
                COMPLETED_CREDIT_PATTERN.matcher(smsBody).find()

        // 4a. Ignore pure balance / statement-due alerts (e.g. HDFC daily
        // "Available Bal ... as on yesterday ... Cheques are subject to clearing").
        // Gated on no debit/credit verb so real transactions with an
        // "Avl Lmt / Available Balance" suffix are still kept.
        if (!isDebit && !isCredit) {
            if ((BALANCE_UPDATE_PATTERNS + STATEMENT_DUE_PATTERNS).any { it.matcher(smsBody).find() }) {
                return null
            }
        }

        val isSenderBank = BANK_SENDER_KEYWORDS.any { senderUpper.contains(it) }

        // A trusted-looking sender, card/account mention, and amount are only supporting
        // context. The RAW fallback still requires an explicit transaction term and a
        // positive completion marker.
        val isCompletedRawTransaction = !isDebit && !isCredit && isSenderBank &&
                TRANSACTION_TERM_PATTERN.matcher(smsBody).find() &&
                COMPLETION_PATTERN.matcher(smsBody).find()
        if (!isDebit && !isCredit && !isCompletedRawTransaction) {
            return null
        }

        val isIncome = isCredit && !isDebit
        val txnType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        val confidence = if (isCompletedRawTransaction) ParseConfidence.RAW else ParseConfidence.FULL

        // 5. Extract Merchant / Payee
        var merchant = extractMerchant(smsBody)
        if (merchant.isBlank() || merchant.length > 40 || !isValidMerchant(merchant)) {
            merchant = inferMerchantFromSender(sender, isIncome) ?: if (isIncome) "Bank Credit / Dividend" else "Bank / Card Expense"
        }

        // 6. Detect Payment Mode & Card Last 4
        val cardLast4 = extractCardLastFour(smsBody)
        val (modeType, modeName) = if (isIncome) null to null else detectPaymentMode(smsBody)

        return ParsedSmsTransaction(
            amount = amount,
            merchant = cleanMerchantName(merchant),
            paymentModeType = modeType,
            paymentModeName = modeName,
            transactionType = txnType,
            rawBody = smsBody,
            sender = sender,
            timestamp = timestamp,
            parseConfidence = confidence,
            cardLastFour = cardLast4,
            currencyCode = currencyCode
        )
    }

    private fun extractMerchant(body: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(body)
            while (matcher.find()) {
                val candidate = matcher.group(1)?.trim() ?: continue
                val clean = cleanCandidateMerchant(candidate)
                if (isValidMerchant(clean)) {
                    return clean
                }
            }
        }
        return ""
    }

    private fun cleanCandidateMerchant(candidate: String): String {
        var clean = candidate
            .replace(Regex("(?i)^(ach\\*|upi/|imps/|neft/|inf\\*|ach-|vpa\\s+)"), "")
            .replace(Regex("(?i)\\b(for|ref|txn|avl|bal|upi|inr|rs|ending|using|via|lmt)\\b"), "")
            .trim()

        // If it looks like a UPI ID (e.g. swiggy@icici or 9876543210@paytm or john.doe@oksbi)
        if (clean.contains("@")) {
            val prefix = clean.substringBefore("@").replace(".", " ").trim()
            // If prefix is not purely numeric (phone number UPI), use the prefix
            if (!prefix.matches(Regex("^[0-9]+$")) && prefix.length >= 2) {
                clean = prefix
            }
        }

        return clean.trim()
    }

    private fun isValidMerchant(name: String): Boolean {
        if (name.length < 2 || name.length > 40) return false
        if (name.contains("http", ignoreCase = true)) return false
        if (NUMERIC_OR_AMOUNT_REGEX.matches(name)) return false
        if (name.matches(Regex("^[0-9\\.,\\s\\-_]+$"))) return false
        val lower = name.lowercase()
        if (INVALID_WORDS.any { lower == it || lower.startsWith(it) }) return false
        return true
    }

    private fun detectPaymentMode(body: String): Pair<PaymentModeType, String> {
        val lower = body.lowercase()

        val isCreditCard = lower.contains("credit card") || lower.contains("creditcard") ||
                (lower.contains("card") && !lower.contains("debit card") && !lower.contains("account") && !lower.contains("a/c"))

        val isBankAccount = BANK_ACCOUNT_PATTERN.matcher(body).find() || lower.contains("account") ||
                lower.contains("a/c") || lower.contains("acct") || lower.contains("debit card") ||
                lower.contains("debited from a/c")

        if (isCreditCard && !isBankAccount) {
            return PaymentModeType.CREDIT_CARD to "Credit Card"
        }

        // All UPI, Bank Accounts, Debit Cards, APAY UPI -> DEBIT
        return PaymentModeType.BANK_DEBIT to "DEBIT"
    }

    private fun inferMerchantFromSender(sender: String?, isCredit: Boolean): String? {
        if (sender == null) return null
        val upper = sender.uppercase()
        val action = if (isCredit) "Credit" else "Spend"
        return when {
            upper.contains("ICICI") -> "ICICI Bank $action"
            upper.contains("HDFC") -> "HDFC Bank $action"
            upper.contains("SBI") -> "SBI $action"
            upper.contains("AXIS") -> "Axis Bank $action"
            upper.contains("KOTAK") -> "Kotak $action"
            upper.contains("AMAZON") || upper.contains("APAY") -> "Amazon Pay"
            else -> null
        }
    }

    private fun cleanMerchantName(raw: String): String {
        return raw.trim()
            .replace(Regex("^[\\W_*]+|[\\W_*]+$"), "")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
    }
}
