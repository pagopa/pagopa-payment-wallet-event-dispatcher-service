package it.pagopa.wallet.eventdispatcher.common.queue

import it.pagopa.wallet.eventdispatcher.common.cdc.LoggingEvent

data class CdcQueueEvent<T : LoggingEvent>(val data: T, val tracingInfo: TracingInfo? = null)
