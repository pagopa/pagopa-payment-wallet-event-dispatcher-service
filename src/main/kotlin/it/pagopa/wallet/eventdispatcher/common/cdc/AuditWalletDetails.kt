package it.pagopa.wallet.eventdispatcher.common.cdc

/** Data class that contains wallet details typologies, such as CARDS, for a log event */
data class AuditWalletDetails(val type: String, val cardBrand: String?, val pspId: String?)