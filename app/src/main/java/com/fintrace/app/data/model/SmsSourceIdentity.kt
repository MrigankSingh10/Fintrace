package com.fintrace.app.data.model

data class SmsSourceIdentity(
    val body: String,
    val sender: String?,
    val sourceTimestamp: Long,
    val receivedTimestamp: Long
) {
    fun matchesLegacyTransactionTimestamp(timestamp: Long): Boolean =
        timestamp == sourceTimestamp || timestamp == receivedTimestamp
}
