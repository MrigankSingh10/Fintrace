package com.fintrace.app

import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.util.ExcelWorkbookGenerator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class ExcelWorkbookGeneratorTest {

    @Test
    fun recurringColumnPreservesExistingColumnsAndMarksEachTransaction() {
        val xml = transactionSheet(
            listOf(
                TransactionWithDetails(
                    transaction = TransactionEntity(
                        id = 1,
                        description = "Rent",
                        timestamp = 1_700_000_000_000L,
                        originalAmount = 1200.0,
                        myShareAmount = 1200.0,
                        categoryId = 1,
                        paymentModeId = 1,
                        isRecurring = true
                    ),
                    category = null,
                    paymentMode = null
                ),
                TransactionWithDetails(
                    transaction = TransactionEntity(
                        id = 2,
                        description = "Coffee",
                        timestamp = 1_700_000_000_000L,
                        originalAmount = 150.0,
                        myShareAmount = 150.0,
                        categoryId = 1,
                        paymentModeId = 1
                    ),
                    category = null,
                    paymentMode = null
                )
            )
        )

        assertEquals("Notes", cellText(xml, "J1"))
        assertEquals("Recurring?", cellText(xml, "K1"))
        assertEquals("Yes", cellText(xml, "K2"))
        assertEquals("No", cellText(xml, "K3"))
        assertEquals("Rent", cellText(xml, "B2"))
        assertEquals("Coffee", cellText(xml, "B3"))
        assertEquals("A1:K1", xml.documentElement
            .getElementsByTagNameNS(SPREADSHEET_NS, "autoFilter")
            .item(0)
            .attributes
            .getNamedItem("ref")
            .nodeValue)
    }

    @Test
    fun emptyExportIncludesRecurringHeaderAndFilterRange() {
        val xml = transactionSheet(emptyList())

        assertEquals("Notes", cellText(xml, "J1"))
        assertEquals("Recurring?", cellText(xml, "K1"))
        assertEquals("A1:K1", xml.documentElement
            .getElementsByTagNameNS(SPREADSHEET_NS, "autoFilter")
            .item(0)
            .attributes
            .getNamedItem("ref")
            .nodeValue)
    }

    private fun transactionSheet(transactions: List<TransactionWithDetails>) =
        workbookEntries(transactions)["xl/worksheets/sheet1.xml"]!!
            .toByteArray(Charsets.UTF_8)
            .let { bytes ->
                DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
                    .newDocumentBuilder()
                    .parse(ByteArrayInputStream(bytes))
            }

    private fun cellText(document: org.w3c.dom.Document, reference: String): String {
        val cell = document.getElementsByTagNameNS(SPREADSHEET_NS, "c").let { cells ->
            (0 until cells.length)
                .map(cells::item)
                .first { it.attributes.getNamedItem("r").nodeValue == reference } as org.w3c.dom.Element
        }
        return cell.getElementsByTagNameNS(SPREADSHEET_NS, "t").item(0).textContent
    }

    private fun workbookEntries(transactions: List<TransactionWithDetails>): Map<String, String> {
        val output = ByteArrayOutputStream()
        ExcelWorkbookGenerator.generateXlsx(
            outputStream = output,
            transactions = transactions,
            categoryBreakdown = emptyList(),
            paymentModeBreakdown = emptyList(),
            timeframeLabel = "Test"
        )
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return entries
    }

    private companion object {
        const val SPREADSHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    }
}
