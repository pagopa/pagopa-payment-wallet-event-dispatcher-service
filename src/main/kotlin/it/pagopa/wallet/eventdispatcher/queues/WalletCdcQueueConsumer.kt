package it.pagopa.wallet.eventdispatcher.queues

import com.azure.core.util.BinaryData
import com.azure.core.util.serializer.JsonSerializerProvider
import com.azure.core.util.serializer.TypeReference
import com.azure.spring.messaging.AzureHeaders
import com.azure.spring.messaging.checkpoint.Checkpointer
import it.pagopa.wallet.eventdispatcher.common.cdc.*
import it.pagopa.wallet.eventdispatcher.common.queue.CdcQueueEvent
import it.pagopa.wallet.eventdispatcher.configuration.QueueConsumerConfiguration
import it.pagopa.wallet.eventdispatcher.service.WalletCDCService
import it.pagopa.wallet.eventdispatcher.utils.Tracing
import it.pagopa.wallet.eventdispatcher.utils.TracingKeys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class WalletCdcQueueConsumer(
    @Qualifier("cdcAzureJsonSerializer") azureJsonSerializer: JsonSerializerProvider,
    private val tracing: Tracing,
    @Autowired private val walletCDCService: WalletCDCService
) {
    private val azureSerializer = azureJsonSerializer.createInstance()

    companion object {
        const val INPUT_CHANNEL = QueueConsumerConfiguration.WALLET_CDC_CHANNEL
        private val EVENT_TYPE_REFERENCE = object : TypeReference<CdcQueueEvent<LoggingEvent>>() {}
    }

    private val logger = LoggerFactory.getLogger(WalletCdcQueueConsumer::class.java)
    private val consumerSpanName = WalletCdcQueueConsumer::class.java.simpleName

    @ServiceActivator(inputChannel = INPUT_CHANNEL, outputChannel = "nullChannel")
    fun messageReceiver(
        @Payload payload: ByteArray,
        @Header(AzureHeaders.CHECKPOINTER) checkPointer: Checkpointer
    ): Mono<Unit> {
        return checkPointer
            .successWithLog() // TODO move to the end?
            .flatMap {
                BinaryData.fromBytes(payload).toObjectAsync(EVENT_TYPE_REFERENCE, azureSerializer)
            }
            .flatMap {
                tracing.traceMonoWithRemoteSpan(consumerSpanName, it.tracingInfo) {
                    handleCdcEvent(it.data)
                }
            }
            .doOnError { error ->
                logger.error("Exception processing wallet expiration event", error)
            }
            .thenReturn(Unit)
    }

    private fun handleCdcEvent(event: LoggingEvent): Mono<Unit> {

        return tracing.customizeSpan(
            if (event is WalletLoggingEvent) {
                Mono.defer { walletCDCService.sendToKafka(event) }
                    .onErrorResume {
                        logger.error(
                            "Error while processing event with id [{}] of type [{}] with walletId [{}] published on [{}]",
                            event.id,
                            event.type,
                            event.walletId,
                            event.timestamp
                        )
                        Mono.empty()
                    }
            } else {
                Mono.just(logger.debug("Not a valid event"))
            }
        ) {
            setAttribute(TracingKeys.CDC_EVENT_ID_KEY, event.id)
            setAttribute(TracingKeys.CDC_WALLET_EVENT_TYPE_KEY, event::class.java.simpleName)
        }
    }
}
