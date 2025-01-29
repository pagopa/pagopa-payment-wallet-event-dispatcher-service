package it.pagopa.wallet.eventdispatcher.warmup.utils

import it.pagopa.wallet.eventdispatcher.common.cdc.WarmupLoggingEvent
import it.pagopa.wallet.eventdispatcher.domain.WalletCreatedEvent
import java.time.OffsetDateTime

object EventsUtil {
    fun getWalletCreatedEvent(): WalletCreatedEvent {
        return WalletCreatedEvent(
            eventId = "00000000-0000-0000-0000-000000000001",
            creationDate = OffsetDateTime.now(),
            walletId = "00000000-0000-0000-0000-000000000000"
        )
    }

    fun getWarmupLoggingEvent(): WarmupLoggingEvent {
        return WarmupLoggingEvent()
    }
}
