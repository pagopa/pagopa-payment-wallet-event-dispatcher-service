package it.pagopa.wallet.eventdispatcher.warmup.utils

import com.azure.spring.messaging.checkpoint.Checkpointer
import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.wallet.eventdispatcher.common.cdc.LoggingEvent
import it.pagopa.wallet.eventdispatcher.common.queue.CdcQueueEvent
import it.pagopa.wallet.eventdispatcher.common.queue.QueueEvent
import it.pagopa.wallet.eventdispatcher.configuration.CdcSerializationConfiguration
import it.pagopa.wallet.eventdispatcher.configuration.SerializationConfiguration
import it.pagopa.wallet.eventdispatcher.domain.WalletEvent
import reactor.core.publisher.Mono

object DummyCheckpointer : Checkpointer {
    override fun success(): Mono<Void> = Mono.empty()
    override fun failure(): Mono<Void> = Mono.empty()
}

object WarmupRequests {

    // CDC serialization utils
    private val cdcSerializationConfiguration = CdcSerializationConfiguration()
    private val cdcObjectMapperBuilder = cdcSerializationConfiguration.cdcObjectMapperBuilder()
    private val cdcObjectMapper: ObjectMapper =
        cdcSerializationConfiguration.cdcObjectMapper(cdcObjectMapperBuilder)

    // Wallet serialization utils
    private val serializationConfiguration = SerializationConfiguration()
    private val objectMapperBuilder = serializationConfiguration.objectMapperBuilder()
    private val objectMapper: ObjectMapper =
        serializationConfiguration.objectMapper(objectMapperBuilder)

    // for WalletCreatedEvent
    fun getWalletCreatedEvent(): ByteArray {
        val event = EventsUtil.getWalletCreatedEvent()
        return traceAndSerializeWalletEvent(event)
    }

    // for WalletLoggingEvent subtype
    fun getWalletDeletedEvent(): ByteArray {
        val event = EventsUtil.getWarmupLoggingEvent()
        return traceAndSerializeCdcEvent(event)
    }

    private fun traceAndSerializeCdcEvent(event: LoggingEvent): ByteArray {
        val queueEvent = CdcQueueEvent(event, null)
        val jsonString = cdcObjectMapper.writeValueAsString(queueEvent)

        return traceAndSerializeEvent(jsonString)
    }

    private fun traceAndSerializeWalletEvent(event: WalletEvent): ByteArray {
        val queueEvent = QueueEvent(event, null)
        val jsonString = objectMapper.writeValueAsString(queueEvent)

        return traceAndSerializeEvent(jsonString)
    }

    private fun traceAndSerializeEvent(stringEvent: String): ByteArray {
        // Replace the "tracingInfo": null with the desired structure
        val tracingInfoReplacement =
            """
        "tracingInfo": {
          "traceparent": "00-5868efa082297543570dafff7d53c70b-56f1d9262e6ee6cf-00",
          "tracestate": null,
          "baggage": null
        }
    """
                .trimIndent()

        // Use regular expression or string replacement to perform the substitution
        val jsonString = stringEvent.replace("\"tracingInfo\":null", tracingInfoReplacement)
        return jsonString.toByteArray()
    }
}
