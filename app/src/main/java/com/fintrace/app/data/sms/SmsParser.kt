package com.fintrace.app.data.sms

import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.TransactionType
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsTransaction(
    val amount: Double,
    val merchant: String,
    val paymentModeType: PaymentModeType,
    val paymentModeName: String,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val rawBody: String,
    val sender: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object SmsParser {

    // Regex for transaction amounts (e.g. INR 200, Rs. 1,500.50, Rs 500, INR 45.00)
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?:INR|RS\\.?|₹)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Keywords indicating debit/spend
    private val DEBIT_KEYWORDS = listOf(
        "debited", "spent", "paid", "charged", "withdrawn", "txn of", "purchase of", "sent to"
    )

    // Keywords indicating credit/income
    private val CREDIT_KEYWORDS = listOf(
        "credited", "refund", "received from", "salary", "cashback"
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

    // High-priority merchant extraction patterns (ordered from most specific to general)
    private val MERCHANT_PATTERNS = listOf(
        // "; Zepto credited." or "; Umed Singh Goud credited. UPI:..."
        Pattern.compile("(?:;|\\.|\\b)\\s*([A-Za-z0-9\\.\\-_@&/ ]+?)\\s+credited(?:\\.|;|\$|\\s+upi|\\s+ref|\\s+call)", Pattern.CASE_INSENSITIVE),
        // "at SWIGGY on 28-Aug" or "at AMAZON INDIA."
        Pattern.compile("\\bat\\s+([A-Za-z0-9\\.\\-_@&/ ]+?)(?:\\s+on|\\s+ref|\\s+avl|\\s+bal|\\s+tot|\\s+using|\\s+limit|\\.|\$|;)", Pattern.CASE_INSENSITIVE),
        // "paid to SWIGGY on" / "transfer to John on" / "sent to John"
        Pattern.compile("(?:paid|transferred|transfer|sent)\\s+to\\s+([A-Za-z0-9\\.\\-_@&/ ]+?)(?:\\s+on|\\s+ref|\\s+using|\\s+via|\\s+avl|\\s+bal|\\.|\$|;)", Pattern.CASE_INSENSITIVE),
        // "spent on SWIGGY on" / "spent at SWIGGY" / "towards SWIGGY on"
        Pattern.compile("(?:spent\\s+(?:on|at)|towards)\\s+([A-Za-z0-9\\.\\-_@&/ ]+?)(?:\\s+on|\\s+using|\\s+via|\\s+avl|\\s+bal|\\.|\$|;)", Pattern.CASE_INSENSITIVE),
        // "vpa swiggy@icici on"
        Pattern.compile("\\bvpa\\s+([A-Za-z0-9\\.\\-_@&]+)", Pattern.CASE_INSENSITIVE),
        // "to VPA swiggy@icici" or "to merchant xyz"
        Pattern.compile("\\bto\\s+(?:vpa\\s+)?([A-Za-z0-9\\.\\-_@& ]+?)(?:\\s+on|\\s+ref|\\s+avl|\\s+bal|\\s+tot|\\s+using|\\.|\$|;)", Pattern.CASE_INSENSITIVE),
        // "info/SWIGGY/1234" or "UPI/SWIGGY/123"
        Pattern.compile("(?:info|inf|desc)[\\*:/\\s]+(?:ach\\*|upi/|imps/|neft/)?([A-Za-z0-9\\.\\-_@& ]+?)(?:\\*[0-9]+|/[0-9]+|\\s+on|\\s+ref|\\s+avl|\\s+bal|\\.|\$|;)", Pattern.CASE_INSENSITIVE),
        // "UPI/123456/SWIGGY"
        Pattern.compile("(?:upi|imps|neft)/[0-9]+/([A-Za-z0-9\\.\\-_@& ]+?)(?:/|\\s+on|\\s+ref|\\s+avl|\\.|\$|;)", Pattern.CASE_INSENSITIVE)
    )

    // Regex to detect purely numeric or amount string that should NEVER be a merchant
    private val NUMERIC_OR_AMOUNT_REGEX = Regex("^(?:INR|RS\\.?|₹)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?\\s*$", RegexOption.IGNORE_CASE)
    private val INVALID_WORDS = listOf(
        "a transaction of", "transaction of", "card ending", "account ending", "your account",
        "your card", "credit card", "debit card", "bank", "atm", "purchase of", "payment of",
        "for inr", "for rs", "by inr", "by rs", "avl lmt", "avl bal", "ref no", "upi ref"
    )

    // Card pattern (e.g. Card ending 1234 or Card XX1234)
    private val CARD_PATTERN = Pattern.compile(
        "(?:credit\\s*card|card)\\s*(?:ending|no|\\s)*[xX*]*([0-9]{4})",
        Pattern.CASE_INSENSITIVE
    )

    // Bank Account pattern (e.g. Account XX857, A/C 1234, Bank Account)
    private val BANK_ACCOUNT_PATTERN = Pattern.compile(
        "(?:account|a/c|acct)\\s*[xX*]*([0-9]{3,4})",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(smsBody: String, sender: String? = null, timestamp: Long = System.currentTimeMillis()): ParsedSmsTransaction? {
        val lower = smsBody.lowercase()

        // 1. Filter out OTPs or non-financial messages
        if (IGNORE_KEYWORDS.any { lower.contains(it) }) {
            return null
        }

        // 2. Ignore Credit Card Bill Payment receipts (e.g. "payment received towards your credit card")
        if (CARD_BILL_PAYMENT_PATTERNS.any { it.matcher(smsBody).find() }) {
            return null
        }

        // 3. Check for Financial Action (Debit or Credit)
        val isDebit = DEBIT_KEYWORDS.any { lower.contains(it) }
        val isCredit = CREDIT_KEYWORDS.any { lower.contains(it) }

        if (!isDebit && !isCredit) {
            return null
        }

        // 4. Extract Amount
        val amountMatcher = AMOUNT_PATTERN.matcher(smsBody)
        if (!amountMatcher.find()) {
            return null
        }

        val rawAmountStr = amountMatcher.group(1)?.replace(",", "") ?: return null
        val amount = rawAmountStr.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val isIncome = isCredit && !isDebit
        val txnType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // 5. Extract Merchant / Payee
        var merchant = extractMerchant(smsBody)
        if (merchant.isBlank() || merchant.length > 40 || !isValidMerchant(merchant)) {
            merchant = inferMerchantFromSender(sender, isIncome) ?: if (isIncome) "Bank Credit / Dividend" else "Bank / Card Expense"
        }

        // 6. Detect Payment Mode (DEBIT, ICICI Coral, ICICI APAY, HDFC Neu)
        val (modeType, modeName) = detectPaymentMode(smsBody, sender)

        return ParsedSmsTransaction(
            amount = amount,
            merchant = cleanMerchantName(merchant),
            paymentModeType = modeType,
            paymentModeName = modeName,
            transactionType = txnType,
            rawBody = smsBody,
            sender = sender,
            timestamp = timestamp
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

    private fun detectPaymentMode(body: String, sender: String?): Pair<PaymentModeType, String> {
        val lower = body.lowercase()
        val senderUpper = (sender ?: "").uppercase()

        val isCreditCard = lower.contains("credit card") || lower.contains("creditcard") ||
                (lower.contains("card") && !lower.contains("debit card") && !lower.contains("account") && !lower.contains("a/c"))

        val isBankAccount = BANK_ACCOUNT_PATTERN.matcher(body).find() || lower.contains("account") ||
                lower.contains("a/c") || lower.contains("acct") || lower.contains("debit card") ||
                lower.contains("debited from a/c")

        // 1. Check for specific Credit Cards
        if (isCreditCard && !isBankAccount) {
            // ICICI APAY Card
            if (lower.contains("amazon") || lower.contains("apay") || senderUpper.contains("AMAZON")) {
                return PaymentModeType.CREDIT_CARD to "ICICI APAY"
            }
            // ICICI Coral or other ICICI Credit Cards
            if (senderUpper.contains("ICICI") || lower.contains("icici")) {
                return PaymentModeType.CREDIT_CARD to "ICICI Coral"
            }
            // HDFC Neu or other HDFC Credit Cards
            if (senderUpper.contains("HDFC") || lower.contains("hdfc")) {
                return PaymentModeType.CREDIT_CARD to "HDFC Neu"
            }
            return PaymentModeType.CREDIT_CARD to "ICICI Coral"
        }

        // 2. All UPI, Bank Accounts, Debit Cards, APAY UPI -> DEBIT
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
