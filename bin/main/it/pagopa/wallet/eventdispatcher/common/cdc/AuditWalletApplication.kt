package it.pagopa.wallet.eventdispatcher.common.cdc

import com.fasterxml.jackson.annotation.JsonProperty

/** Data class that contains wallet application details for a log event */
data class AuditWalletApplication(
    @JsonProperty("_id") val id: String,
    val status: String,
    val creationDate: String,
    val updateDate: String,
    val metadata: Map<String, String?>
) {}
