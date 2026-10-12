package com.fintrace.app

import com.fintrace.app.ui.dashboard.categoryGlanceToggleLabel
import com.fintrace.app.ui.transactions.splitBillHeaderLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpandableSectionPresentationTest {
    @Test
    fun splitHeaderTextExplainsTheActionForEachState() {
        assertEquals("Split bill — tap to add people", splitBillHeaderLabel(isExpanded = false, hasSavedSplit = false))
        assertEquals("Split bill — tap to edit shares", splitBillHeaderLabel(isExpanded = false, hasSavedSplit = true))
        assertEquals("Split bill — tap to collapse", splitBillHeaderLabel(isExpanded = true, hasSavedSplit = true))
    }

    @Test
    fun categoryHeaderAccessibilityLabelTracksExpandedState() {
        assertEquals("Collapse top categories", categoryGlanceToggleLabel(isExpanded = true))
        assertEquals("Expand top categories", categoryGlanceToggleLabel(isExpanded = false))
    }
}
