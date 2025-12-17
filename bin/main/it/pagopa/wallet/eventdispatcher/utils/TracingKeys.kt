package it.pagopa.wallet.eventdispatcher.utils

import io.opentelemetry.api.common.AttributeKey

object TracingKeys {

    val PATCH_STATE_WALLET_ID_KEY = AttributeKey.stringKey("wallet.patch.state.walletId")
    val PATCH_STATE_TRIGGER_KEY = AttributeKey.stringKey("wallet.patch.state.trigger")
    val PATCH_STATE_OUTCOME_KEY = AttributeKey.stringKey("wallet.patch.state.outcome")
    val PATCH_STATE_OUTCOME_FAIL_STATUS_CODE_KEY =
        AttributeKey.stringKey("wallet.patch.state.outcome.fail.code")
    val CDC_WALLET_ID_KEY = AttributeKey.stringKey("wallet.cdc.event.walletId")
    val CDC_EVENT_ID_KEY = AttributeKey.stringKey("wallet.cdc.event.eventId")
    val CDC_WALLET_EVENT_TYPE_KEY = AttributeKey.stringKey("wallet.cdc.event.type")

    enum class WalletPatchTriggerKind {
        WALLET_EXPIRE
    }

    enum class WalletPatchOutcome {
        OK,
        FAIL,
    }
}
