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
        assertEquals("HDFC Neu", parsed.paymentModeName)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
    }

    @Test
    fun testIciciCoralCreditCardSms() {
        val sms = "Dear Customer, txn of INR 200.00 done on ICICI Bank Card XX9876 at UBER TRIP on 18-Aug-26. Avl Limit: INR 1,20,000.00"
        val parsed = SmsParser.parse(sms, "ICICIB")

        assertNotNull(parsed)
        assertEquals(200.0, parsed!!.amount, 0.001)
        assertEquals("Uber Trip", parsed.merchant)
        assertEquals("ICICI Coral", parsed.paymentModeName)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
    }

    @Test
    fun testIciciApayCreditCardSms() {
        val sms = "Alert: Spent INR 1,299.00 on your Amazon Pay ICICI Bank Credit Card ending 3344 at Amazon on 18-AUG-2026."
        val parsed = SmsParser.parse(sms, "ICICIB")

        assertNotNull(parsed)
        assertEquals(1299.0, parsed!!.amount, 0.001)
        assertEquals("Amazon", parsed.merchant)
        assertEquals("ICICI APAY", parsed.paymentModeName)
        assertEquals(PaymentModeType.CREDIT_CARD, parsed.paymentModeType)
    }

    @Test
    fun testIciciBankDividendIncomeSms() {
        val sms = "ICICI Bank Account XX857 credited:Rs. 17.00 on 18-Aug-26. Info ACH*COAL INDIA LTD*937306. Available Balance is Rs. 9,62,504.24."
        val parsed = SmsParser.parse(sms, "BG-ICICIT-S")

        assertNotNull(parsed)
        assertEquals(17.0, parsed!!.amount, 0.001)
        assertEquals("Coal India Ltd", parsed.merchant)
        assertEquals("DEBIT", parsed.paymentModeName)
        assertEquals(PaymentModeType.BANK_DEBIT, parsed.paymentModeType)
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
}
