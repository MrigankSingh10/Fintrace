package com.fintrace.app

import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.data.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun testRawParseConfidenceWhenNoDebitCreditKeywords() {
        // SMS from a bank sender with an amount, but unusual wording lacking standard debit/credit keywords
        val rawSms = "SBI Bank notice: INR 500.00 charged on your Card ending 1122 at CAFE COFFEE DAY"
        val parsed = SmsParser.parse(rawSms, "SBIBNK")

        assertNotNull("Should parse SMS with detectable amount", parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals("1122", parsed.cardLastFour)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
    }

    @Test
    fun testBankSenderRawMessageWithAmount() {
        val sms = "HDFC Bank Alert: Total due for statement is Rs. 4,320.00 for your Card XX5566"
        val parsed = SmsParser.parse(sms, "HDFCBK")

        assertNotNull(parsed)
        assertEquals(4320.0, parsed!!.amount, 0.001)
        assertEquals("5566", parsed.cardLastFour)
        assertEquals(com.fintrace.app.data.model.ParseConfidence.RAW, parsed.parseConfidence)
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
}
