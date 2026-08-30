package com.fintrace.app.util

import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.ui.analytics.PaymentModeSpendSummary
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generates a native Excel (.xlsx) workbook using raw OpenXML / ZIP.
 * No external library required — works on Android minSdk 26+.
 *
 * Sheet 1: Transactions
 * Sheet 2: Pivot Summary (Category table + Card split table + native embedded doughnut chart)
 */
object ExcelWorkbookGenerator {

    private val dateFmt = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault())

    private fun fmt(d: Double) = "%.2f".format(d)

    private fun esc(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private val cols = listOf("A","B","C","D","E","F","G","H","I","J")
    private fun ref(col: Int, row: Int) = "${cols[col]}$row"

    /** Inline string cell */
    private fun c(ref: String, value: String, styleId: Int = 0) =
        """<c r="$ref" t="inlineStr" s="$styleId"><is><t>${esc(value)}</t></is></c>"""

    /** Numeric cell */
    private fun n(ref: String, value: Double, styleId: Int = 0) =
        """<c r="$ref" s="$styleId"><v>${fmt(value)}</v></c>"""

    /** Int cell */
    private fun ni(ref: String, value: Int, styleId: Int = 0) =
        """<c r="$ref" s="$styleId"><v>$value</v></c>"""

    // ── Public entry point ────────────────────────────────────────────────────

    fun generateXlsx(
        outputStream: OutputStream,
        transactions: List<TransactionWithDetails>,
        categoryBreakdown: List<CategorySpendSummary>,
        paymentModeBreakdown: List<PaymentModeSpendSummary>,
        timeframeLabel: String
    ) {
        val sheet1Xml  = buildSheet1(transactions)
        val sheet2Xml  = buildSheet2(categoryBreakdown, paymentModeBreakdown, timeframeLabel)
        val chartXml   = buildChartXml(categoryBreakdown)
        val drawingXml = buildDrawingXml()
        val stylesXml  = buildStyles()

        ZipOutputStream(outputStream).use { zip ->
            zip.writeEntry("[Content_Types].xml",                   buildContentTypes())
            zip.writeEntry("_rels/.rels",                           buildRootRels())
            zip.writeEntry("xl/workbook.xml",                       buildWorkbook())
            zip.writeEntry("xl/_rels/workbook.xml.rels",            buildWorkbookRels())
            zip.writeEntry("xl/styles.xml",                         stylesXml)
            zip.writeEntry("xl/sharedStrings.xml",                  buildEmptySst())
            zip.writeEntry("xl/worksheets/sheet1.xml",              sheet1Xml)
            zip.writeEntry("xl/worksheets/sheet2.xml",              sheet2Xml)
            zip.writeEntry("xl/worksheets/_rels/sheet2.xml.rels",   buildSheet2Rels())
            zip.writeEntry("xl/drawings/drawing1.xml",              drawingXml)
            zip.writeEntry("xl/drawings/_rels/drawing1.xml.rels",   buildDrawingRels())
            zip.writeEntry("xl/charts/chart1.xml",                  chartXml)
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHEET 1: Transactions
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildSheet1(transactions: List<TransactionWithDetails>): String {
        val rows = StringBuilder()
        val headers = listOf(
            "Date & Time","Description / Merchant","Category","Payment Mode",
            "Type","My Share (Rs)","Original Amount (Rs)","Is Split?","Split Breakdown","Notes"
        )
        rows.append("<row r=\"1\">")
        headers.forEachIndexed { ci, h -> rows.append(c(ref(ci, 1), h, 2)) }
        rows.append("</row>")

        transactions.forEachIndexed { idx, item ->
            val row = idx + 2
            val t = item.transaction
            val dateStr = dateFmt.format(Date(t.timestamp))
            val catName = item.category?.name ?: "Uncategorized"
            val modeName = item.paymentMode?.name ?: "Unknown"
            val splitStr = if (item.splits.isNotEmpty())
                item.splits.joinToString(" | ") { "${it.personName}: Rs${fmt(it.shareAmount)}" }
            else "Solo"

            rows.append("<row r=\"$row\">")
            rows.append(c(ref(0, row), dateStr))
            rows.append(c(ref(1, row), t.description))
            rows.append(c(ref(2, row), catName))
            rows.append(c(ref(3, row), modeName))
            rows.append(c(ref(4, row), t.type.label))
            rows.append(n(ref(5, row), t.myShareAmount, 4))
            rows.append(n(ref(6, row), t.originalAmount, 4))
            rows.append(c(ref(7, row), if (item.isSplit) "Yes" else "No"))
            rows.append(c(ref(8, row), splitStr))
            rows.append(c(ref(9, row), t.notes ?: ""))
            rows.append("</row>")
        }

        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """)
            append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
            append("""<sheetViews><sheetView workbookViewId="0"><selection activeCell="A1"/></sheetView></sheetViews>""")
            append("""<cols>""")
            append("""<col min="1" max="1" width="20" customWidth="1"/>""")
            append("""<col min="2" max="2" width="28" customWidth="1"/>""")
            append("""<col min="3" max="3" width="16" customWidth="1"/>""")
            append("""<col min="4" max="4" width="18" customWidth="1"/>""")
            append("""<col min="5" max="5" width="12" customWidth="1"/>""")
            append("""<col min="6" max="6" width="18" customWidth="1"/>""")
            append("""<col min="7" max="7" width="20" customWidth="1"/>""")
            append("""<col min="8" max="8" width="10" customWidth="1"/>""")
            append("""<col min="9" max="9" width="36" customWidth="1"/>""")
            append("""<col min="10" max="10" width="28" customWidth="1"/>""")
            append("""</cols>""")
            append("""<sheetData>$rows</sheetData>""")
            append("""<autoFilter ref="A1:J1"/>""")
            append("""</worksheet>""")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHEET 2: Pivot Summary & Chart
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildSheet2(
        categories: List<CategorySpendSummary>,
        paymentModes: List<PaymentModeSpendSummary>,
        timeframeLabel: String
    ): String {
        val rows = StringBuilder()
        var row = 1

        // Title
        rows.append("""<row r="$row"><c r="A$row" t="inlineStr" s="5"><is><t>Finance Tracker -- Summary Report ($timeframeLabel)</t></is></c></row>""")
        row++
        rows.append("""<row r="$row"/>"""); row++ // spacer

        // Category Pivot Table header
        rows.append("<row r=\"$row\">")
        rows.append(c("A$row", "Category", 2))
        rows.append(c("B$row", "My Share (Rs)", 2))
        rows.append(c("C$row", "% of Total", 2))
        rows.append(c("D$row", "Total Charged (Rs)", 2))
        rows.append("</row>")
        row++

        val totalMyShare = categories.sumOf { it.totalMyShareSpent }
        val totalOriginal = categories.sumOf { it.totalOriginalSpent }

        categories.forEach { cat ->
            val pct = if (totalMyShare > 0) (cat.totalMyShareSpent / totalMyShare * 100) else 0.0
            rows.append("<row r=\"$row\">")
            rows.append(c("A$row", cat.categoryName))
            rows.append(n("B$row", cat.totalMyShareSpent, 4))
            rows.append(c("C$row", "${"%.1f".format(pct)}%"))
            rows.append(n("D$row", cat.totalOriginalSpent, 4))
            rows.append("</row>")
            row++
        }

        // Grand total
        rows.append("<row r=\"$row\">")
        rows.append(c("A$row", "Grand Total", 3))
        rows.append(n("B$row", totalMyShare, 6))
        rows.append(c("C$row", "100.0%", 3))
        rows.append(n("D$row", totalOriginal, 6))
        rows.append("</row>")
        row += 3  // spacer rows

        // Card / Payment Mode Split Table header
        rows.append("<row r=\"$row\">")
        rows.append(c("A$row", "Card / Payment Mode", 2))
        rows.append(c("B$row", "Total Billed (Rs)", 2))
        rows.append(c("C$row", "My Share (Rs)", 2))
        rows.append(c("D$row", "Split Owed to You (Rs)", 2))
        rows.append(c("E$row", "Transactions", 2))
        rows.append("</row>")
        row++

        paymentModes.forEach { mode ->
            rows.append("<row r=\"$row\">")
            rows.append(c("A$row", mode.paymentModeName))
            rows.append(n("B$row", mode.totalOriginalSpent, 4))
            rows.append(n("C$row", mode.totalMyShareSpent, 4))
            rows.append(n("D$row", mode.totalSplitOwed, 4))
            rows.append(ni("E$row", mode.count))
            rows.append("</row>")
            row++
        }

        val cardTotalBilled = paymentModes.sumOf { it.totalOriginalSpent }
        val cardTotalShare  = paymentModes.sumOf { it.totalMyShareSpent }
        val cardTotalOwed   = paymentModes.sumOf { it.totalSplitOwed }
        val cardTotalCount  = paymentModes.sumOf { it.count }
        rows.append("<row r=\"$row\">")
        rows.append(c("A$row", "Grand Total", 3))
        rows.append(n("B$row", cardTotalBilled, 6))
        rows.append(n("C$row", cardTotalShare, 6))
        rows.append(n("D$row", cardTotalOwed, 6))
        rows.append(ni("E$row", cardTotalCount, 3))
        rows.append("</row>")

        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """)
            append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
            append("""<sheetViews><sheetView workbookViewId="0"><selection activeCell="A1"/></sheetView></sheetViews>""")
            append("""<cols>""")
            append("""<col min="1" max="1" width="26" customWidth="1"/>""")
            append("""<col min="2" max="2" width="18" customWidth="1"/>""")
            append("""<col min="3" max="3" width="14" customWidth="1"/>""")
            append("""<col min="4" max="4" width="24" customWidth="1"/>""")
            append("""<col min="5" max="5" width="14" customWidth="1"/>""")
            append("""</cols>""")
            append("""<sheetData>$rows</sheetData>""")
            append("""<drawing r:id="rId1"/>""")
            append("""</worksheet>""")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHART XML — Doughnut chart linked to Category Pivot (A4:B<N>)
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildChartXml(categories: List<CategorySpendSummary>): String {
        val catCount   = categories.size
        val dataEndRow = 3 + catCount  // category data starts at row 4

        val labelRef = "Sheet2!\$A\$4:\$A\$$dataEndRow"
        val valRef   = "Sheet2!\$B\$4:\$B\$$dataEndRow"

        val labelPts = buildString {
            categories.forEachIndexed { i, cat ->
                append("""<c:pt idx="$i"><c:v>${esc(cat.categoryName)}</c:v></c:pt>""")
            }
        }
        val valPts = buildString {
            categories.forEachIndexed { i, cat ->
                append("""<c:pt idx="$i"><c:v>${fmt(cat.totalMyShareSpent)}</c:v></c:pt>""")
            }
        }

        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<c:chartSpace xmlns:c="http://schemas.openxmlformats.org/drawingml/2006/chart" """)
            append("""xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" """)
            append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
            append("""<c:chart>""")
            append("""<c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/>""")
            append("""<a:p><a:r><a:t>Category Spend Breakdown (My Share)</a:t></a:r></a:p>""")
            append("""</c:rich></c:tx><c:overlay val="0"/></c:title>""")
            append("""<c:autoTitleDeleted val="0"/>""")
            append("""<c:plotArea>""")
            append("""<c:doughnutChart>""")
            append("""<c:varyColors val="1"/>""")
            append("""<c:ser>""")
            append("""<c:idx val="0"/><c:order val="0"/>""")
            append("""<c:cat><c:strRef><c:f>$labelRef</c:f>""")
            append("""<c:strCache><c:ptCount val="$catCount"/>$labelPts</c:strCache>""")
            append("""</c:strRef></c:cat>""")
            append("""<c:val><c:numRef><c:f>$valRef</c:f>""")
            append("""<c:numCache><c:formatCode>General</c:formatCode>""")
            append("""<c:ptCount val="$catCount"/>$valPts</c:numCache>""")
            append("""</c:numRef></c:val>""")
            append("""</c:ser>""")
            append("""<c:holeSize val="50"/>""")
            append("""</c:doughnutChart>""")
            append("""</c:plotArea>""")
            append("""<c:legend><c:legendPos val="r"/><c:overlay val="0"/></c:legend>""")
            append("""<c:plotVisOnly val="1"/>""")
            append("""</c:chart>""")
            append("""</c:chartSpace>""")
        }
    }

    // Drawing anchor: chart positioned at F3:N23 on Sheet 2
    private fun buildDrawingXml() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" """)
        append("""xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" """)
        append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("""<xdr:twoCellAnchor editAs="oneCell">""")
        append("""<xdr:from><xdr:col>5</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>2</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>""")
        append("""<xdr:to><xdr:col>13</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>22</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>""")
        append("""<xdr:graphicFrame macro=""><xdr:nvGraphicFramePr>""")
        append("""<xdr:cNvPr id="2" name="Chart 1"/><xdr:cNvGraphicFramePr/></xdr:nvGraphicFramePr>""")
        append("""<xdr:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/></xdr:xfrm>""")
        append("""<a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/chart">""")
        append("""<c:chart xmlns:c="http://schemas.openxmlformats.org/drawingml/2006/chart" r:id="rId1"/>""")
        append("""</a:graphicData></a:graphic></xdr:graphicFrame><xdr:clientData/>""")
        append("""</xdr:twoCellAnchor></xdr:wsDr>""")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Styles: 7 cell styles
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildStyles() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("""<fonts count="3">""")
        append("""<font><sz val="10"/><name val="Calibri"/></font>""")
        append("""<font><b/><sz val="10"/><name val="Calibri"/></font>""")
        append("""<font><b/><sz val="13"/><name val="Calibri"/></font>""")
        append("""</fonts>""")
        append("""<fills count="3">""")
        append("""<fill><patternFill patternType="none"/></fill>""")
        append("""<fill><patternFill patternType="gray125"/></fill>""")
        append("""<fill><patternFill patternType="solid"><fgColor rgb="FF1E3A5F"/></patternFill></fill>""")
        append("""</fills>""")
        append("""<borders count="2">""")
        append("""<border><left/><right/><top/><bottom/><diagonal/></border>""")
        append("""<border>""")
        append("""<left style="thin"><color rgb="FF475569"/></left>""")
        append("""<right style="thin"><color rgb="FF475569"/></right>""")
        append("""<top style="thin"><color rgb="FF475569"/></top>""")
        append("""<bottom style="thin"><color rgb="FF475569"/></bottom>""")
        append("""<diagonal/></border>""")
        append("""</borders>""")
        append("""<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""")
        append("""<cellXfs count="7">""")
        // 0: normal bordered
        append("""<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>""")
        // 1: plain
        append("""<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""")
        // 2: header (bold, dark bg, white font via fill)
        append("""<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>""")
        // 3: bold total
        append("""<xf numFmtId="0" fontId="1" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"/>""")
        // 4: currency normal
        append("""<xf numFmtId="4" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>""")
        // 5: title (large bold)
        append("""<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>""")
        // 6: bold currency (grand total)
        append("""<xf numFmtId="4" fontId="1" fillId="0" borderId="1" xfId="0" applyFont="1" applyNumberFormat="1" applyBorder="1"/>""")
        append("""</cellXfs>""")
        append("""</styleSheet>""")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OpenXML infrastructure
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildEmptySst() =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="0" uniqueCount="0"/>"""

    private fun buildContentTypes() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml"  ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        append("""<Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        append("""<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>""")
        append("""<Override PartName="/xl/drawings/drawing1.xml" ContentType="application/vnd.openxmlformats-officedocument.drawing+xml"/>""")
        append("""<Override PartName="/xl/charts/chart1.xml" ContentType="application/vnd.openxmlformats-officedocument.drawingml.chart+xml"/>""")
        append("""</Types>""")
    }

    private fun buildRootRels() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        append("""<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""")
        append("""</Relationships>""")
    }

    private fun buildWorkbook() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """)
        append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("""<sheets>""")
        append("""<sheet name="Transactions" sheetId="1" r:id="rId1"/>""")
        append("""<sheet name="Pivot Summary" sheetId="2" r:id="rId2"/>""")
        append("""</sheets></workbook>""")
    }

    private fun buildWorkbookRels() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        append("""<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""")
        append("""<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>""")
        append("""<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("""<Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>""")
        append("""</Relationships>""")
    }

    private fun buildSheet2Rels() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        append("""<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/drawing1.xml"/>""")
        append("""</Relationships>""")
    }

    private fun buildDrawingRels() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        append("""<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart" Target="../charts/chart1.xml"/>""")
        append("""</Relationships>""")
    }
}
