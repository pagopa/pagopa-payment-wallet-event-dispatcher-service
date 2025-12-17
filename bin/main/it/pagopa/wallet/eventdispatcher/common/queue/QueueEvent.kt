package it.pagopa.wallet.eventdispatcher.common.queue

import it.pagopa.wallet.eventdispatcher.domain.WalletEvent

data class QueueEvent<T : WalletEvent>(val data: T, val tracingInfo: TracingInfo? = null)
