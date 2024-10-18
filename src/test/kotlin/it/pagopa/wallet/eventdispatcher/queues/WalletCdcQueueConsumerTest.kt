package it.pagopa.wallet.eventdispatcher.queues

import com.azure.spring.messaging.checkpoint.Checkpointer
import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import it.pagopa.wallet.eventdispatcher.common.cdc.*
import it.pagopa.wallet.eventdispatcher.common.queue.CdcQueueEvent
import it.pagopa.wallet.eventdispatcher.common.queue.TracingInfo
import it.pagopa.wallet.eventdispatcher.configuration.CdcSerializationConfiguration
import it.pagopa.wallet.eventdispatcher.service.WalletCDCService
import it.pagopa.wallet.eventdispatcher.utils.Tracing
import it.pagopa.wallet.eventdispatcher.utils.TracingKeys
import it.pagopa.wallet.eventdispatcher.utils.TracingMock
import java.io.ByteArrayOutputStream
import java.util.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class WalletCdcQueueConsumerTest {

    private val checkPointer: Checkpointer = mock()
    private val serializationConfiguration = CdcSerializationConfiguration()
    private val objectMapper: ObjectMapper =
        serializationConfiguration.cdcObjectMapperBuilder().build()
    private val azureJsonSerializer =
        serializationConfiguration.cdcAzureJsonSerializer(objectMapper)
    private val tracing: Tracing = TracingMock.mock()
    private val walletCDCService: WalletCDCService = mock()

    private val walletCdcQueueConsumer =
        WalletCdcQueueConsumer(
            azureJsonSerializer = azureJsonSerializer,
            tracing = tracing,
            walletCDCService
        )

    @Test
    fun `Should process wallet onboard completed event successfully`() {
        val walletId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val creationDate = "2024-06-14T15:04:56.908428Z"
        val walletOnboardCompletedEvent =
            CdcQueueEvent(
                tracingInfo = TracingInfo(),
                data =
                    WalletOnboardCompletedEvent(
                        id = eventId.toString(),
                        timestamp = creationDate,
                        walletId = walletId.toString(),
                        auditWallet =
                            AuditWallet(
                                paymentMethodId = "9d735400-9450-4f7e-9431-8c1e7fa2a339",
                                creationDate = creationDate,
                                updateDate = creationDate,
                                applications =
                                    listOf(
                                        AuditWalletApplication(
                                            "PAGOPA",
                                            "ENABLED",
                                            "2024-10-16T15:03:18.378746985Z",
                                            "2024-10-16T15:03:18.378747385Z",
                                            emptyMap()
                                        )
                                    ),
                                details = AuditWalletDetails("PAYPAL", null, "BCITITMM"),
                                status = "VALIDATED",
                                validationOperationId = "618534471407042909",
                                validationOperationResult = "EXECUTED",
                                validationOperationTimestamp = "2024-10-16T15:03:35.841Z",
                                validationErrorCode = null
                            )
                    )
            )
        given(checkPointer.success()).willReturn(Mono.empty())
        StepVerifier.create(
                walletCdcQueueConsumer.messageReceiver(
                    payload = serializeEvent(walletOnboardCompletedEvent),
                    checkPointer = checkPointer
                )
            )
            .expectNext(Unit)
            .verifyComplete()
        verify(checkPointer, times(1)).success()

        val span =
            argumentCaptor<Span.() -> Unit> {
                    verify(tracing, times(1)).customizeSpan(any<Mono<*>>(), capture())
                }
                .reduceSpan()

        verifySpanAttributes(
            span,
            TracingKeys.CDC_EVENT_ID_KEY to eventId.toString(),
            TracingKeys.CDC_WALLET_EVENT_TYPE_KEY to
                WalletOnboardCompletedEvent::class.java.simpleName
        )
    }

    @Test
    fun `Should ignore default logging event successfully`() {
        val eventId = UUID.randomUUID()
        val creationDate = "2024-06-14T15:04:56.908428Z"
        val loggingEvent =
            CdcQueueEvent(
                tracingInfo = TracingInfo(),
                data = LoggingEvent(id = eventId.toString(), timestamp = creationDate)
            )
        given(checkPointer.success()).willReturn(Mono.empty())
        StepVerifier.create(
                walletCdcQueueConsumer.messageReceiver(
                    payload = serializeEvent(loggingEvent),
                    checkPointer = checkPointer
                )
            )
            .expectNext(Unit)
            .verifyComplete()
        verify(checkPointer, times(1)).success()

        val span =
            argumentCaptor<Span.() -> Unit> {
                    verify(tracing, times(1)).customizeSpan(any<Mono<*>>(), capture())
                }
                .reduceSpan()

        verifySpanAttributes(
            span,
            TracingKeys.CDC_EVENT_ID_KEY to eventId.toString(),
            TracingKeys.CDC_WALLET_EVENT_TYPE_KEY to LoggingEvent::class.java.simpleName
        )
    }

    private fun <T> serializeEvent(event: T): ByteArray {
        val baos = ByteArrayOutputStream()
        azureJsonSerializer.createInstance().serialize(baos, event)
        return baos.toByteArray()
    }

    private fun KArgumentCaptor<Span.() -> Unit>.reduceSpan(): Span {
        return allValues.fold(mock<Span>()) { span, it ->
            it.invoke(span)
            span
        }
    }

    private fun verifySpanAttributes(
        span: Span,
        vararg attributes: Pair<AttributeKey<String>, String>,
    ) {
        attributes.forEach { (t, u) -> verify(span, times(1)).setAttribute(eq(t), eq(u)) }
    }
}
