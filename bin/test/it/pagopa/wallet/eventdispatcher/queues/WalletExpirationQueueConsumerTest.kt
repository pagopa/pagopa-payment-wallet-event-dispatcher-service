package it.pagopa.wallet.eventdispatcher.queues

import com.azure.spring.messaging.checkpoint.Checkpointer
import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import it.pagopa.generated.wallets.model.WalletStatus
import it.pagopa.generated.wallets.model.WalletStatusErrorPatchRequest
import it.pagopa.generated.wallets.model.WalletStatusErrorPatchRequestDetails
import it.pagopa.wallet.eventdispatcher.api.WalletsApi
import it.pagopa.wallet.eventdispatcher.common.queue.QueueEvent
import it.pagopa.wallet.eventdispatcher.configuration.SerializationConfiguration
import it.pagopa.wallet.eventdispatcher.domain.WalletCreatedEvent
import it.pagopa.wallet.eventdispatcher.exceptions.WalletPatchStatusError
import it.pagopa.wallet.eventdispatcher.utils.Tracing
import it.pagopa.wallet.eventdispatcher.utils.TracingKeys
import it.pagopa.wallet.eventdispatcher.utils.TracingMock
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.*
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class WalletExpirationQueueConsumerTest {

    private val checkPointer: Checkpointer = mock()
    private val serializationConfiguration = SerializationConfiguration()
    private val objectMapper: ObjectMapper =
        serializationConfiguration.objectMapperBuilder().build()
    private val azureJsonSerializer = serializationConfiguration.azureJsonSerializer(objectMapper)
    private val walletsApi: WalletsApi = mock()
    private val tracing: Tracing = TracingMock.mock()

    private val walletExpirationQueueConsumer =
        WalletExpirationQueueConsumer(
            azureJsonSerializer = azureJsonSerializer,
            walletsApi = walletsApi,
            tracing = tracing
        )

    @Test
    fun `Should process wallet create event successfully`() {
        val walletId = UUID.randomUUID()
        val creationDate = "2024-06-14T15:04:56.908428Z"
        val walletCreationDate = OffsetDateTime.parse(creationDate)
        val walletCreatedEvent =
            QueueEvent(
                WalletCreatedEvent(
                    walletId = walletId.toString(),
                    eventId = UUID.randomUUID().toString(),
                    creationDate = walletCreationDate
                )
            )
        given(checkPointer.success()).willReturn(Mono.empty())
        given(walletsApi.updateWalletStatus(any(), any())).willReturn(mono {})
        val expectedPatchRequest =
            WalletStatusErrorPatchRequest()
                .status("ERROR")
                .details(
                    WalletStatusErrorPatchRequestDetails()
                        .reason("Wallet expired. Creation date: $creationDate")
                )
        StepVerifier.create(
                walletExpirationQueueConsumer.messageReceiver(
                    payload = serializeEvent(walletCreatedEvent),
                    checkPointer = checkPointer
                )
            )
            .expectNext(Unit)
            .verifyComplete()
        verify(checkPointer, times(1)).success()
        verify(walletsApi, times(1))
            .updateWalletStatus(
                walletId = walletId,
                walletStatusPatchRequest = expectedPatchRequest
            )

        val span =
            argumentCaptor<Span.() -> Unit> {
                    verify(tracing, times(2)).customizeSpan(any<Mono<*>>(), capture())
                }
                .reduceSpan()

        verifySpanAttributes(
            span,
            TracingKeys.PATCH_STATE_WALLET_ID_KEY to walletId.toString(),
            TracingKeys.PATCH_STATE_TRIGGER_KEY to
                TracingKeys.WalletPatchTriggerKind.WALLET_EXPIRE.name,
            TracingKeys.PATCH_STATE_OUTCOME_KEY to TracingKeys.WalletPatchOutcome.OK.name
        )
    }

    @Test
    fun `Should propagate error patching wallet status to ERROR`() {
        val walletId = UUID.randomUUID()
        val creationDate = "2024-06-14T15:04:56.908428Z"
        val walletCreationDate = OffsetDateTime.parse(creationDate)
        val walletCreatedEvent =
            QueueEvent(
                WalletCreatedEvent(
                    walletId = walletId.toString(),
                    eventId = UUID.randomUUID().toString(),
                    creationDate = walletCreationDate
                )
            )
        given(checkPointer.success()).willReturn(Mono.empty())
        given(walletsApi.updateWalletStatus(any(), any()))
            .willReturn(Mono.error(RuntimeException("Error patching wallet")))
        val expectedPatchRequest =
            WalletStatusErrorPatchRequest()
                .status("ERROR")
                .details(
                    WalletStatusErrorPatchRequestDetails()
                        .reason("Wallet expired. Creation date: $creationDate")
                )
        StepVerifier.create(
                walletExpirationQueueConsumer.messageReceiver(
                    payload = serializeEvent(walletCreatedEvent),
                    checkPointer = checkPointer
                )
            )
            .expectErrorMatches {
                assertEquals("Error patching wallet", it.message)
                true
            }
            .verify()
        verify(checkPointer, times(1)).success()
        verify(walletsApi, times(1))
            .updateWalletStatus(
                walletId = walletId,
                walletStatusPatchRequest = expectedPatchRequest
            )

        val span =
            argumentCaptor<Span.() -> Unit> {
                    verify(tracing, times(2)).customizeSpan(any<Mono<*>>(), capture())
                }
                .reduceSpan()

        verifySpanAttributes(
            span,
            TracingKeys.PATCH_STATE_WALLET_ID_KEY to walletId.toString(),
            TracingKeys.PATCH_STATE_TRIGGER_KEY to
                TracingKeys.WalletPatchTriggerKind.WALLET_EXPIRE.name,
            TracingKeys.PATCH_STATE_OUTCOME_KEY to TracingKeys.WalletPatchOutcome.FAIL.name,
            TracingKeys.PATCH_STATE_OUTCOME_FAIL_STATUS_CODE_KEY to ""
        )
    }

    @Test
    fun `Should propagate error patching wallet status to ERROR with status code`() {
        val walletId = UUID.randomUUID()
        val creationDate = "2024-06-14T15:04:56.908428Z"
        val walletCreationDate = OffsetDateTime.parse(creationDate)
        val walletCreatedEvent =
            QueueEvent(
                WalletCreatedEvent(
                    walletId = walletId.toString(),
                    eventId = UUID.randomUUID().toString(),
                    creationDate = walletCreationDate
                )
            )
        given(checkPointer.success()).willReturn(Mono.empty())
        given(walletsApi.updateWalletStatus(any(), any()))
            .willReturn(
                Mono.error(
                    WalletPatchStatusError(
                        walletId,
                        WalletStatus.ERROR.name,
                        WebClientResponseException(404, "not found", HttpHeaders(), null, null)
                    )
                )
            )
        val expectedPatchRequest =
            WalletStatusErrorPatchRequest()
                .status("ERROR")
                .details(
                    WalletStatusErrorPatchRequestDetails()
                        .reason("Wallet expired. Creation date: $creationDate")
                )
        StepVerifier.create(
                walletExpirationQueueConsumer.messageReceiver(
                    payload = serializeEvent(walletCreatedEvent),
                    checkPointer = checkPointer
                )
            )
            .expectError()
            .verify()
        verify(checkPointer, times(1)).success()
        verify(walletsApi, times(1))
            .updateWalletStatus(
                walletId = walletId,
                walletStatusPatchRequest = expectedPatchRequest
            )

        val span =
            argumentCaptor<Span.() -> Unit> {
                    verify(tracing, times(2)).customizeSpan(any<Mono<*>>(), capture())
                }
                .reduceSpan()

        verifySpanAttributes(
            span,
            TracingKeys.PATCH_STATE_WALLET_ID_KEY to walletId.toString(),
            TracingKeys.PATCH_STATE_TRIGGER_KEY to
                TracingKeys.WalletPatchTriggerKind.WALLET_EXPIRE.name,
            TracingKeys.PATCH_STATE_OUTCOME_KEY to TracingKeys.WalletPatchOutcome.FAIL.name,
            TracingKeys.PATCH_STATE_OUTCOME_FAIL_STATUS_CODE_KEY to "404"
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "{}",
                """
         {
                "data": {
                    "type": "WalletExpired",
                    "eventId": "bcfb7296-c53f-4840-9977-84a597fca1a0",
                    "creationDate": 1718206463.077352000
                },
                "tracingInfo": {
                    "traceparent": "traceparent",
                    "tracestate": "tracestate",
                    "baggage": "baggage"
                }
            }
    """
            ]
    )
    fun `Should perform success checkpoint for invalid event`(queueEvent: String) {
        val payload = queueEvent.toByteArray(StandardCharsets.UTF_8)
        given(checkPointer.success()).willReturn(Mono.empty())
        StepVerifier.create(
                walletExpirationQueueConsumer.messageReceiver(
                    payload = payload,
                    checkPointer = checkPointer
                )
            )
            .expectError()
            .verify()
        verify(checkPointer, times(1)).success()
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
