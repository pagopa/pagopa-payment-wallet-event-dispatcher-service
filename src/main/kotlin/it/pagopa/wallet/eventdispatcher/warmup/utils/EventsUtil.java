package it.pagopa.wallet.eventdispatcher.warmup.utils;

import it.pagopa.wallet.eventdispatcher.common.cdc.WalletDeletedEvent;
import it.pagopa.wallet.eventdispatcher.domain.WalletCreatedEvent;

import java.time.OffsetDateTime;

public class EventsUtil {
    public static WalletCreatedEvent getWalletCreatedEvent() {
        return new WalletCreatedEvent(
                "00000000-0000-0000-0000-000000000001",
                OffsetDateTime.now(),
                "00000000-0000-0000-0000-000000000000"
        );
    }

    public static WalletDeletedEvent getWalletDeleteLoggingEvent() {
        return new WalletDeletedEvent(
                "00000000-0000-0000-0000-000000000001",
                "2025-01-10T14:28:47.843515440Z[Etc/UTC]",
                WalletDeletedEvent.class.getSimpleName(),
                "00000000-0000-0000-0000-000000000000"
        );
    }

}
