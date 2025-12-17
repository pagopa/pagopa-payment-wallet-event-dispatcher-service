package it.pagopa.wallet.eventdispatcher.common.queue

data class TracingInfo(
    val traceparent: String = "",
    val tracestate: String? = "",
    val baggage: String? = ""
)
