package it.pagopa.wallet.eventdispatcher.service

import it.pagopa.wallet.eventdispatcher.common.cdc.AuditWallet
import it.pagopa.wallet.eventdispatcher.common.cdc.AuditWalletApplication
import it.pagopa.wallet.eventdispatcher.common.cdc.AuditWalletDetails
import it.pagopa.wallet.eventdispatcher.common.cdc.WalletOnboardCompletedEvent
import it.pagopa.wallet.eventdispatcher.configuration.properties.RetrySendPolicyConfig
import java.time.Instant
import java.util.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import reactor.core.publisher.Mono
import reactor.kafka.sender.SenderResult
import reactor.kotlin.test.test

@ExtendWith(MockitoExtension::class)
class WalletCDCServiceTest {

    private val retrySendPolicyConfig: RetrySendPolicyConfig = RetrySendPolicyConfig(1, 100)
    private val cdcKafkaTemplate: ReactiveKafkaProducerTemplate<String, Any> = mock()
    private val cdcTopicName: String = "test-topic"
    private val walletCDCService =
        WalletCDCService(cdcKafkaTemplate, cdcTopicName, retrySendPolicyConfig)

    private val sampleApplications =
        listOf(
            AuditWalletApplication(
                id = UUID.randomUUID().toString(),
                status = "ACTIVE",
                creationDate = Instant.now().toString(),
                updateDate = Instant.now().toString(),
                metadata = mapOf("key1" to "value1", "key2" to "value2")
            )
        )

    private val sampleDetails =
        AuditWalletDetails(type = "CARDS", cardBrand = "VISA", pspId = "psp123")

    private val auditWallet =
        AuditWallet(
            paymentMethodId = UUID.randomUUID().toString(),
            creationDate = Instant.now().toString(),
            updateDate = Instant.now().toString(),
            applications = sampleApplications,
            details = sampleDetails,
            status = "VALID",
            validationOperationId = UUID.randomUUID().toString(),
            validationOperationResult = "SUCCESS",
            validationOperationTimestamp = Instant.now().toString(),
            validationErrorCode = null
        )

    private val event =
        WalletOnboardCompletedEvent(
            id = UUID.randomUUID().toString(),
            walletId = UUID.randomUUID().toString(),
            timestamp = Instant.now().toString(),
            auditWallet = auditWallet
        )

    @Test
    fun `should successfully send CDC event to Kafka`() {

        val sendResult = mock<SenderResult<Void>>()
        given(cdcKafkaTemplate.send(anyString(), anyString(), any()))
            .willReturn(Mono.just(sendResult))

        walletCDCService.sendToKafka(event).test().assertNext { it is Unit }.verifyComplete()
    }

    @Test
    fun `should log error when failing to send CDC event to Kafka`() {

        given(cdcKafkaTemplate.send(anyString(), anyString(), any()))
            .willAnswer { Mono.error<RuntimeException>(RuntimeException("First attempt failed")) }
            .willAnswer { Mono.error<RuntimeException>(RuntimeException("Second attempt failed")) }
        walletCDCService
            .sendToKafka(event)
            .test()
            .expectErrorMatches { it is RuntimeException }
            .verify()
    }

    @Test
    fun `should dispatch event from on second retry`() {

        val sendResult = mock<SenderResult<Void>>()
        given(cdcKafkaTemplate.send(anyString(), anyString(), any()))
            .willAnswer { Mono.error<RuntimeException>(RuntimeException("First attempt failed")) }
            .willReturn(Mono.just(sendResult))

        walletCDCService.sendToKafka(event).test().assertNext { it is Unit }.verifyComplete()
    }
}
