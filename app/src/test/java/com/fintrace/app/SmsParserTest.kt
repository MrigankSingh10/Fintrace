package com.fintrace.app

import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.ParseConfidence
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.data.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun testHdfcNeuCreditCardSms() {
        val sms = "Alert: You've spent INR 1,450.00 on HDFC Bank Credit Card XX4321 at SWIGGY on 18-AUG-2026. Avl Lmt: INR 85,000.00"
        val parsed = SmsParser.parse(sms, "HDFCBK")

        assertNotNull("SMS should be parsed", parsed)
        assertEquals(1450.0, parsed!!.amount, 0.001)
        assertEquals("Swiggy", parsed.merchant)
        assertEquals("Credit Card", parsed.paymentModeName)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals("4321", parsed.cardLastFour)
        assertEquals(com.fintrace.app.data.model.ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals("INR", parsed.currencyCode)
    }

    @Test
    fun testIciciCoralCreditCardSms() {
        val sms = "Dear Customer, txn of INR 200.00 done on ICICI Bank Card XX9876 at UBER TRIP on 18-Aug-26. Avl Limit: INR 1,20,000.00"
        val parsed = SmsParser.parse(sms, "ICICIB")

        assertNotNull(parsed)
        assertEquals(200.0, parsed!!.amount, 0.001)
        assertEquals("Uber Trip", parsed.merchant)
        assertEquals("Credit Card", parsed.paymentModeName)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals("9876", parsed.cardLastFour)
        assertEquals(com.fintrace.app.data.model.ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
    }

    @Test
    fun testIciciApayCreditCardSms() {
        val sms = "Alert: Spent INR 1,299.00 on your Amazon Pay ICICI Bank Credit Card ending 3344 at Amazon on 18-AUG-2026."
        val parsed = SmsParser.parse(sms, "ICICIB")

        assertNotNull(parsed)
        assertEquals(1299.0, parsed!!.amount, 0.001)
        assertEquals("Amazon", parsed.merchant)
        assertEquals("Credit Card", parsed.paymentModeName)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals("3344", parsed.cardLastFour)
        assertEquals(com.fintrace.app.data.model.ParseConfidence.FULL, parsed.parseConfidence)
    }

    @Test
    fun testIciciBankDividendIncomeSms() {
        val sms = "ICICI Bank Account XX857 credited:Rs. 17.00 on 18-Aug-26. Info ACH*COAL INDIA LTD*937306. Available Balance is Rs. 9,62,504.24."
        val parsed = SmsParser.parse(sms, "BG-ICICIT-S")

        assertNotNull(parsed)
        assertEquals(17.0, parsed!!.amount, 0.001)
        assertEquals("Coal India Ltd", parsed.merchant)
        assertNull(parsed.paymentModeName)
        assertNull(parsed.paymentModeType)
        assertEquals(TransactionType.INCOME, parsed.transactionType)
    }

    @Test
    fun testCreditCardBillPaymentExclusion() {
        val billPaymentSms = "DEAR HDFCBANK CARDMEMBER, PAYMENT OF Rs. 5157.00 RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH 6279 ON 15-8-2026.YOUR AVAILABLE LIMIT IS RS. 215999.90"
        val parsed = SmsParser.parse(billPaymentSms, "HDFCBK")

        assertNull("Credit card bill payment must be ignored as an expense", parsed)
    }

    @Test
    fun testUpiDebitTreatedAsDebit() {
        val sms = "Dear UPI user A/C 6789 debited by INR 450.00 on 18Aug26 transfer to Zomato UPI ref no 987654321."
        val parsed = SmsParser.parse(sms, "SBIUPI")

        assertNotNull(parsed)
        assertEquals(450.0, parsed!!.amount, 0.001)
        assertEquals("Zomato", parsed.merchant)
        assertEquals("DEBIT", parsed.paymentModeName)
        assertEquals(PaymentModeType.BANK_DEBIT, parsed.paymentModeType)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
    }

    @Test
    fun testHdfcUpiSentFromAccountToPayeeTreatedAsDebit() {
        val sms = "Sent Rs.167.00\nFrom HDFC Bank A/C *1234\nTo JUSTSWISH\nOn 26/09/26\nRef 1xxxxxxxxx\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808"
        val parsed = SmsParser.parse(sms, "HDFCBK")

        assertNotNull("Sent-from-account UPI debit must be parsed", parsed)
        assertEquals(167.0, parsed!!.amount, 0.001)
        assertEquals("Justswish", parsed.merchant)
        assertEquals("DEBIT", parsed.paymentModeName)
        assertEquals(PaymentModeType.BANK_DEBIT, parsed.paymentModeType)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals("INR", parsed.currencyCode)
    }

    @Test
    fun testIciciDebitPayeeCredited() {
        val sms = "ICICI Bank Acct XX857 debited for Rs 28.00 on 28-Aug-26; Umed Singh Goud credited. UPI:660662384392. Call 18002662 for dispute. SMS BLOCK 857 to 9215676766."
        val parsed = SmsParser.parse(sms, "ICICIB")

        assertNotNull(parsed)
        assertEquals(28.0, parsed!!.amount, 0.001)
        assertEquals("Umed Singh Goud", parsed.merchant)
        assertEquals("DEBIT", parsed.paymentModeName)
        assertEquals(PaymentModeType.BANK_DEBIT, parsed.paymentModeType)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
    }

    @Test
    fun testIciciDebitZeptoCredited() {
        val sms = "ICICI Bank Acct XX857 debited for Rs 706.45 on 28-Aug-26; Zepto credited. UPI:201812291812. Call 18002662 for dispute. SMS BLOCK 857 to 9215676766."
        val parsed = SmsParser.parse(sms, "ICICIB")

        assertNotNull(parsed)
        assertEquals(706.45, parsed!!.amount, 0.001)
        assertEquals("Zepto", parsed.merchant)
        assertEquals("DEBIT", parsed.paymentModeName)
        assertEquals(PaymentModeType.BANK_DEBIT, parsed.paymentModeType)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
    }

    @Test
    fun testOtpExclusion() {
        val otpSms = "123456 is your OTP for purchase of INR 2,000.00 at Flipkart. Never share OTP with anyone."
        val parsed = SmsParser.parse(otpSms, "HDFCBK")

        assertNull("OTP message must be ignored", parsed)
    }

    @Test
    fun testChargedCardSmsUsesFullConfidence() {
        // "Charged" is explicit debit evidence, so this must not use the RAW fallback.
        val rawSms = "SBI Bank notice: INR 500.00 charged on your Card ending 1122 at CAFE COFFEE DAY"
        val parsed = SmsParser.parse(rawSms, "SBIBNK")

        assertNotNull("Charged card SMS should parse", parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals("1122", parsed.cardLastFour)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
    }

    @Test
    fun testBankSenderRawMessageWithAmount() {
        // Statement-due alerts move no money and must be ignored as transactions.
        val sms = "HDFC Bank Alert: Total due for statement is Rs. 4,320.00 for your Card XX5566"
        val parsed = SmsParser.parse(sms, "HDFCBK")

        assertNull("Statement-due alert must be ignored", parsed)
    }

    @Test
    fun testBobcardScapiaGbpTransaction() {
        val sms = "Your txn of GBP28.00 AT HOLAFLY WAS SUCCESSFUL ON YOUR BOBCARD SCAPIA Credit Card ending with 2313.Not you? Go to Scapia support on the app or call 1800 2090.-BOBCARD"
        val parsed = SmsParser.parse(sms, "BOBCARD")

        assertNotNull("Scapia transaction should be parsed", parsed)
        assertEquals(28.0, parsed!!.amount, 0.001)
        assertEquals("Holafly", parsed.merchant)
        assertEquals("2313", parsed.cardLastFour)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals("Credit Card", parsed.paymentModeName)
        assertEquals(com.fintrace.app.data.model.ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals("GBP", parsed.currencyCode)
    }

    @Test
    fun testHdfcSpentBankCardWithFooterSms() {
        val sms = "Spent Rs. 8471 On HDFC Bank Card 6392 At TATA STARBUCKS PRIVATE\nOn 2025-11-23:09:41:07.Not You? To Block+Reissue Call 18007654321/\nSMS BLOCK CC 6392 to "
        val parsed = SmsParser.parse(sms, "HDFCBK")

        assertNotNull("HDFC spent SMS should parse fully", parsed)
        assertEquals(8471.0, parsed!!.amount, 0.001)
        assertEquals("Tata Starbucks Private", parsed.merchant)
        assertEquals("6392", parsed.cardLastFour)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals(com.fintrace.app.data.model.ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals("INR", parsed.currencyCode)
    }

    @Test
    fun testAmexCardWithFiveDigitTail() {
        val sms = "Alert: You've spent INR 2,318.50 on your AMEX card ** 45219 at\nAMAZON BD on 22 March 2025 at 11:47 AM IST. Call 18005550134\nif this was not made by you. "
        val parsed = SmsParser.parse(sms, "AMEX")

        assertNotNull("AMEX SMS with 5-digit masked tail should parse", parsed)
        assertEquals(2318.5, parsed!!.amount, 0.001)
        assertEquals("Amazon Bd", parsed.merchant)
        assertEquals("45219", parsed.cardLastFour)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals(com.fintrace.app.data.model.ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
    }

    @Test
    fun testHdfcDailyAvailableBalIgnored() {
        val sms = "Available Bal in HDFC Bank A/c XX2745 as on yesterday:22-SEP-26 is INR 6,45,594.37. Cheques are subject to clearing.For updated A/C Bal dial 18002703333."
        val parsed = SmsParser.parse(sms, "HDFCBK")

        assertNull("Daily balance update must be ignored", parsed)
    }

    @Test
    fun testGenericAvlBalWithoutActionIgnored() {
        val sms = "Avl Bal in A/c XX857 is Rs. 50,000.00. Total Avl Lmt is Rs. 2,00,000.00."
        val parsed = SmsParser.parse(sms, "ICICIB")

        assertNull("Balance-only alert must be ignored", parsed)
    }

    @Test
    fun testBalanceAsOnWithoutActionIgnored() {
        val sms = "Your A/c balance as on 22-SEP-26 is INR 6,45,594.37."
        val parsed = SmsParser.parse(sms, "HDFCBK")

        assertNull("Balance-as-on alert must be ignored", parsed)
    }

    @Test
    fun testMinimumDueWithoutActionIgnored() {
        val sms = "Minimum due of Rs. 500.00, payment due date 05-Oct-26 for Card XX5566."
        val parsed = SmsParser.parse(sms, "HDFCBK")

        assertNull("Minimum-due alert must be ignored", parsed)
    }

    @Test
    fun testSpendWithAvlLmtSuffixStillParsed() {
        // Negative control: real spend carrying an Avl Lmt suffix must NOT be ignored,
        // and the first amount (not the limit) is the transaction amount.
        val sms = "Alert: You've spent INR 1,450.00 on HDFC Bank Credit Card XX4321 at SWIGGY on 18-AUG-2026. Avl Lmt: INR 85,000.00"
        val parsed = SmsParser.parse(sms, "HDFCBK")

        assertNotNull("Real spend with Avl suffix must still parse", parsed)
        assertEquals(1450.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals("INR", parsed.currencyCode)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals("4321", parsed.cardLastFour)
    }

    @Test
    fun testCreditWithAvailableBalanceSuffixStillParsed() {
        // Negative control: real credit carrying an Available Balance suffix must NOT be ignored.
        val sms = "ICICI Bank Account XX857 credited:Rs. 17.00 on 18-Aug-26. Info ACH*COAL INDIA LTD*937306. Available Balance is Rs. 9,62,504.24."
        val parsed = SmsParser.parse(sms, "BG-ICICIT-S")

        assertNotNull("Real credit with balance suffix must still parse", parsed)
        assertEquals(17.0, parsed!!.amount, 0.001)
        assertEquals("Coal India Ltd", parsed.merchant)
        assertEquals(TransactionType.INCOME, parsed.transactionType)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals("INR", parsed.currencyCode)
        assertNull(parsed.paymentModeType)
        assertNull(parsed.cardLastFour)
    }

    @Test
    fun testPromotionalCashbackRejected() {
        val parsed = SmsParser.parse("Get ₹500 cashback on your HDFC card. Apply today!", "HDFCBK")

        assertNull("Cashback advertising must not be imported as income", parsed)
    }

    @Test
    fun testPromotionalEmiRejected() {
        val parsed = SmsParser.parse("Convert purchases to EMI of INR 2,000 on your ICICI card.", "ICICIB")

        assertNull("An EMI offer is not a posted expense", parsed)
    }

    @Test
    fun testBankCardOfferWithAmountRejected() {
        val parsed = SmsParser.parse("Exclusive HDFC card offer: save INR 1,000 at Amazon.", "HDFCBK")

        assertNull("Sender, card, and amount are not sufficient transaction evidence", parsed)
    }

    @Test
    fun testPromotionalSalaryAndRefundWordingRejected() {
        assertNull(SmsParser.parse("Unlock salary benefits worth INR 5,000 with your bank account.", "AXISBK"))
        assertNull(SmsParser.parse("Get an instant refund of INR 500 on your next purchase.", "PAYTM"))
    }

    @Test
    fun testPromotionalSaleOfferWithAmountRejected() {
        val sms = "Tira ALMOST EVERYTHING ON SALE access is approved\nYour code is NOWORNEVER\nFlat 20% off + Rs.2000 off\npromotions.tirabeauty.com/GK3mDk2"

        assertNull("Sale promo must not be imported even from a bank-like sender",
            SmsParser.parse(sms, "HDFCBK"))
        assertNull("Sale promo must not be imported from any sender",
            SmsParser.parse(sms, "TIRABE"))
    }

    @Test
    fun testNonPostedTransactionsRejected() {
        assertNull(SmsParser.parse("Transaction of INR 900 on Card XX1122 failed.", "HDFCBK"))
        assertNull(SmsParser.parse("INR 900 was declined on Card XX1122 at SWIGGY.", "HDFCBK"))
        assertNull(SmsParser.parse("Transaction of INR 900 on Card XX1122 was cancelled.", "HDFCBK"))
        assertNull(SmsParser.parse("Transaction of INR 900 on Card XX1122 was canceled.", "HDFCBK"))
        assertNull(SmsParser.parse("Transaction of INR 900 on Card XX1122 was unsuccessful.", "HDFCBK"))
        assertNull(SmsParser.parse("Transaction of INR 900 on Card XX1122 was reversed.", "HDFCBK"))
        assertNull(SmsParser.parse("Reversal of INR 900 for Card XX1122.", "HDFCBK"))
    }

    @Test
    fun testCompletedCashbackRefundAndSalaryCreditsAccepted() {
        val cashback = SmsParser.parse("Cashback of INR 50 credited to your Account XX857.", "ICICIB")
        val refund = SmsParser.parse("Refund of INR 275 has been processed for your card XX1122.", "HDFCBK")
        val salary = SmsParser.parse("Salary of INR 75,000 credited to your Account XX857.", "ICICIB")

        assertNotNull(cashback)
        assertEquals(50.0, cashback!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, cashback.transactionType)
        assertEquals(ParseConfidence.FULL, cashback.parseConfidence)
        assertEquals("INR", cashback.currencyCode)
        assertNull(cashback.paymentModeType)
        assertNull(cashback.cardLastFour)

        assertNotNull(refund)
        assertEquals(275.0, refund!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, refund.transactionType)
        assertEquals(ParseConfidence.FULL, refund.parseConfidence)
        assertEquals("INR", refund.currencyCode)
        assertNull(refund.paymentModeType)
        assertEquals("1122", refund.cardLastFour)

        assertNotNull(salary)
        assertEquals(75000.0, salary!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, salary.transactionType)
        assertEquals(ParseConfidence.FULL, salary.parseConfidence)
        assertEquals("INR", salary.currencyCode)
        assertNull(salary.paymentModeType)
        assertNull(salary.cardLastFour)
    }

    @Test
    fun testCompletedBankTransactionUsesRawConfidence() {
        val parsed = SmsParser.parse(
            "Your card transaction for INR 700 was successful.",
            "SBIBNK"
        )

        assertNotNull(parsed)
        assertEquals(700.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals(ParseConfidence.RAW, parsed.parseConfidence)
        assertEquals("INR", parsed.currencyCode)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals("Credit Card", parsed.paymentModeName)
        assertNull(parsed.cardLastFour)
    }

    @Test
    fun testPantaloonsCashbackPromotionFromEmulatorIgnored() {
        val sms = "Your Pantaloons Greencard exclusive offer expires SOOOON!\n\nGet Rs750 cashback on shopping of Rs1500 in one bill\n\nTC. One time offer. Cashback as points on 4Nov"

        assertNull(SmsParser.parse(sms, "5551236"))
    }

    @Test
    fun testHdfcStatementDueFromEmulatorIgnored() {
        val sms = "HDFC Bank Alert: Total due for statement is Rs. 4,320.00 for your Card XX5566"

        assertNull(SmsParser.parse(sms, "5550101"))
    }

    @Test
    fun testHdfcAvailableBalanceFromEmulatorIgnored() {
        val sms = "Available Bal in HDFC Bank A/c XX1235 as on yesterday:22-SEP-26 is INR 6,45,594.38. Cheques are subject to clearing.For updated A/C Bal dial 18002703333."

        assertNull(SmsParser.parse(sms, "5550100"))
    }

    @Test
    fun testIciciZeptoDebitFromEmulatorParsed() {
        val sms = "ICICI Bank Acct XX137 debited for Rs 706.45; Zepto credited."
        val parsed = SmsParser.parse(sms, "5551236")

        assertNotNull(parsed)
        assertEquals(706.45, parsed!!.amount, 0.001)
        assertEquals("Zepto", parsed.merchant)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals(PaymentModeType.BANK_DEBIT, parsed.paymentModeType)
        assertEquals("DEBIT", parsed.paymentModeName)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals("INR", parsed.currencyCode)
        assertNull(parsed.cardLastFour)
    }

    @Test
    fun testIciciSalaryCreditFromEmulatorParsed() {
        val sms = "ICICI Bank Account XX137 credited with INR 85000.00 as salary on 05-Sep-26. Available balance INR 120000.00."
        val parsed = SmsParser.parse(sms, "5551234")

        assertNotNull(parsed)
        assertEquals(85000.0, parsed!!.amount, 0.001)
        assertEquals("Bank Credit / Dividend", parsed.merchant)
        assertEquals(TransactionType.INCOME, parsed.transactionType)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals("INR", parsed.currencyCode)
        assertNull(parsed.paymentModeType)
        assertNull(parsed.paymentModeName)
        assertNull(parsed.cardLastFour)
    }

    @Test
    fun testIciciDividendCreditFromEmulatorParsed() {
        val sms = "ICICI Bank Account XX137 credited:Rs. 1250.00 on 05-Sep-26. Info ACH*COAL INDIA LTD*937306. Available Balance is Rs. 963754.24."
        val parsed = SmsParser.parse(sms, "5551233")

        assertNotNull(parsed)
        assertEquals(1250.0, parsed!!.amount, 0.001)
        assertEquals("Coal India Ltd", parsed.merchant)
        assertEquals(TransactionType.INCOME, parsed.transactionType)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals("INR", parsed.currencyCode)
        assertNull(parsed.paymentModeType)
        assertNull(parsed.paymentModeName)
        assertNull(parsed.cardLastFour)
    }

    @Test
    fun testBobcardHolaflyTransactionFromEmulatorParsed() {
        val sms = "Your txn of GBP28.00 AT HOLAFLY WAS SUCCESSFUL ON YOUR BOBCARD SCAPIA Credit Card ending with 1243.Not you? Go to Scapia support on the app or call 1800 2090.-BOBCARD"
        val parsed = SmsParser.parse(sms, "5551235")

        assertNotNull(parsed)
        assertEquals(28.0, parsed!!.amount, 0.001)
        assertEquals("Holafly", parsed.merchant)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals("Credit Card", parsed.paymentModeName)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals("GBP", parsed.currencyCode)
        assertEquals("1243", parsed.cardLastFour)
    }

    @Test
    fun testAmexAmazonTransactionFromEmulatorParsed() {
        val sms = "Alert: You've spent INR 1,004.00 on your AMEX card ** 12345 at AMAZON BD on 10 September 2026 at 08:31 PM IST. Call 18004190691 if this was not made by you."
        val parsed = SmsParser.parse(sms, "5551231")

        assertNotNull(parsed)
        assertEquals(1004.0, parsed!!.amount, 0.001)
        assertEquals("Amazon Bd", parsed.merchant)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals("Credit Card", parsed.paymentModeName)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals("INR", parsed.currencyCode)
        assertEquals("12345", parsed.cardLastFour)
    }

    @Test
    fun testHdfcStarbucksTransactionFromEmulatorParsed() {
        val sms = "Spent Rs. 1223 On HDFC Bank Card 4744 At TATA STARBUCKS PRIVATE\nOn 2026-09-06:21:04:49.Not You? To Block+Reissue Call 1800258616 SMS BLOCK CC 4744 to 1234"
        val parsed = SmsParser.parse(sms, "5551232")

        assertNotNull(parsed)
        assertEquals(1223.0, parsed!!.amount, 0.001)
        assertEquals("Tata Starbucks Private", parsed.merchant)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals("Credit Card", parsed.paymentModeName)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        assertEquals("INR", parsed.currencyCode)
        assertEquals("4744", parsed.cardLastFour)
    }

    @Test
    fun testStandaloneBlockedAmountIgnored() {
        assertNull(SmsParser.parse("INR 500 blocked on your card.", "HDFCBK"))
    }

    @Test
    fun testPrepaidDoesNotMatchPaidAction() {
        assertNull(SmsParser.parse("Get a prepaid card worth INR 500 today.", "HDFCBK"))
    }

    @Test
    fun testCompletedTransactionFromUnknownSenderIgnored() {
        assertNull(SmsParser.parse("Your card transaction for INR 700 was successful.", "5559999"))
    }

    @Test
    fun testDirectedPaymentFormsParsedAsExpenses() {
        val messages = listOf(
            "Paid to Zomato INR 100.",
            "Paid INR 500 to Swiggy.",
            "Transferred INR 600 to Uber.",
            "Sent INR 700 to Zepto."
        )

        messages.forEach { sms ->
            val parsed = SmsParser.parse(sms, "HDFCBK")
            assertNotNull("Expected directed payment to parse: $sms", parsed)
            assertEquals(TransactionType.EXPENSE, parsed!!.transactionType)
            assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        }
    }

    @Test
    fun testGenericBankCreditWithUpiReferenceParsedAsIncome() {
        val sms = "Dear Customer, Acct XX123 is credited with Rs 95218.00 on 26-Sep-26 from XXXXXX UPI:1XXXXXXXXXXX-ICICI Bank."
        val parsed = SmsParser.parse(sms, "ICICIB")

        assertNotNull(parsed)
        assertEquals(95218.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, parsed.transactionType)
        assertEquals("INR", parsed.currencyCode)
        assertEquals(ParseConfidence.FULL, parsed.parseConfidence)
        assertNull(parsed.paymentModeType)
    }
}
