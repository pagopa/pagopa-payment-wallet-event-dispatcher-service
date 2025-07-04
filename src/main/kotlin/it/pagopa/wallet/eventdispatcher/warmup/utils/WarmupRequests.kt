package it.pagopa.wallet.eventdispatcher.warmup.utils

import com.azure.spring.messaging.checkpoint.Checkpointer
import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.wallet.eventdispatcher.common.cdc.LoggingEvent
import it.pagopa.wallet.eventdispatcher.common.queue.CdcQueueEvent
import it.pagopa.wallet.eventdispatcher.common.queue.QueueEvent
import it.pagopa.wallet.eventdispatcher.common.queue.TracingInfo
import it.pagopa.wallet.eventdispatcher.configuration.CdcSerializationConfiguration
import it.pagopa.wallet.eventdispatcher.configuration.SerializationConfiguration
import it.pagopa.wallet.eventdispatcher.domain.WalletEvent
import java.nio.charset.StandardCharsets
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

    // for LoggingEvent subtype
    fun getWarmupLoggingEvent(): ByteArray {
        val event = EventsUtil.getWarmupLoggingEvent()
        return traceAndSerializeCdcEvent(event)
    }

    private fun traceAndSerializeCdcEvent(event: LoggingEvent): ByteArray {
        val queueEvent = CdcQueueEvent(event, TracingInfo())
        return cdcObjectMapper.writeValueAsString(queueEvent).toByteArray(StandardCharsets.UTF_8)
    }

    private fun traceAndSerializeWalletEvent(event: WalletEvent): ByteArray {
        val queueEvent = QueueEvent(event, TracingInfo())
        return objectMapper.writeValueAsString(queueEvent).toByteArray(StandardCharsets.UTF_8)
    }
}
