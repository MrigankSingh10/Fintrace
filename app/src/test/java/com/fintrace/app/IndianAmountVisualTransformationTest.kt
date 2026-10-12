package com.fintrace.app

import androidx.compose.ui.text.AnnotatedString
import com.fintrace.app.ui.components.IndianAmountVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndianAmountVisualTransformationTest {
    @Test
    fun groupsIntegerDigitsUsingIndianGroupingAndPreservesInputFormatting() {
        val cases = mapOf(
            "" to "",
            "." to ".",
            "1234." to "1,234.",
            "1234.5000" to "1,234.5000",
            "123456" to "1,23,456",
            "123456789" to "12,34,56,789",
            "-123456.00" to "-1,23,456.00",
            "+0012345.0600" to "+00,12,345.0600",
            "0000123" to "00,00,123"
        )

        cases.forEach { (input, expected) ->
            assertEquals(input, expected, transform(input).text.text)
        }
    }

    @Test
    fun leavesUnsupportedValuesUnchangedWithIdentityOffsets() {
        listOf("1,234", "12.3.4", "1a234", "--1234", "1-234").forEach { input ->
            val result = transform(input)
            assertEquals(input, result.text.text)
            for (offset in 0..input.length) {
                assertEquals(offset, result.offsetMapping.originalToTransformed(offset))
                assertEquals(offset, result.offsetMapping.transformedToOriginal(offset))
            }
        }
    }

    @Test
    fun offsetMappingIsMonotonicAndRoundTripsEveryOriginalBoundary() {
        val inputs = listOf(
            "1234", "12345", "123456", "1234567", "123456789",
            "-123456.78", "+0012345.0600", "1234.", ".", ""
        )

        inputs.forEach { input ->
            val result = transform(input)
            val mapping = result.offsetMapping
            var previous = -1
            for (offset in 0..input.length) {
                val transformedOffset = mapping.originalToTransformed(offset)
                assertTrue("original mapping decreases for $input at $offset", transformedOffset >= previous)
                previous = transformedOffset
                assertEquals(
                    "original round trip failed for $input at $offset",
                    offset,
                    mapping.transformedToOriginal(transformedOffset)
                )
            }

            previous = -1
            for (offset in 0..result.text.length) {
                val originalOffset = mapping.transformedToOriginal(offset)
                assertTrue("transformed mapping decreases for $input at $offset", originalOffset >= previous)
                previous = originalOffset
                val canonicalOffset = mapping.originalToTransformed(originalOffset)
                assertTrue(
                    "transformed boundary moved too far for $input at $offset",
                    canonicalOffset in offset..(offset + 1).coerceAtMost(result.text.length)
                )
            }
        }
    }

    private fun transform(value: String) =
        IndianAmountVisualTransformation.filter(AnnotatedString(value))
}
