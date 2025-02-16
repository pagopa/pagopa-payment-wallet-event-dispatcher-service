package it.pagopa.wallet.eventdispatcher.warmup.utils

import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.wallet.eventdispatcher.common.cdc.WarmupLoggingEvent
import it.pagopa.wallet.eventdispatcher.configuration.CdcSerializationConfiguration
import it.pagopa.wallet.eventdispatcher.configuration.SerializationConfiguration
import it.pagopa.wallet.eventdispatcher.domain.WalletCreatedEvent
import it.pagopa.wallet.eventdispatcher.queues.WalletCdcQueueConsumer
import it.pagopa.wallet.eventdispatcher.queues.WalletExpirationQueueConsumer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier

@TestPropertySource(locations = ["classpath:application.test.properties"])
class WarmupRequestsTest {

    @Test
    fun `Should handle WalletCreatedEvent`() {
        val serializationConfiguration = SerializationConfiguration()
        val objectMapper: ObjectMapper = serializationConfiguration.objectMapperBuilder().build()
        val azureJsonSerializer = serializationConfiguration.azureJsonSerializer(objectMapper)

        val walletExpirationQueueConsumer =
            WalletExpirationQueueConsumer(
                walletsApi = mock(),
                azureJsonSerializer = azureJsonSerializer,
                tracing = mock()
            )
        val payload = WarmupRequests.getWalletCreatedEvent()
        val result = walletExpirationQueueConsumer.parseEvent(payload)

        StepVerifier.create(result)
            .assertNext { queueEvent -> assertTrue(queueEvent.data is WalletCreatedEvent) }
            .verifyComplete()
    }

    @Test
    fun `Should handle WarmupLoggingEvent`() {

        val serializationConfiguration = CdcSerializationConfiguration()
        val objectMapper: ObjectMapper = serializationConfiguration.cdcObjectMapperBuilder().build()
        val cdcAzureJsonSerializer = serializationConfiguration.cdcAzureJsonSerializer(objectMapper)

        val walletCdcQueueConsumer =
            WalletCdcQueueConsumer(
                azureJsonSerializer = cdcAzureJsonSerializer,
                tracing = mock(),
                walletCDCService = mock()
            )

        val payload = WarmupRequests.getWarmupLoggingEvent()
        val result = walletCdcQueueConsumer.parseEvent(payload)

        StepVerifier.create(result)
            .assertNext { queueEvent ->
                assertEquals(WarmupLoggingEvent::class.java.simpleName, queueEvent.data.type)
            }
            .verifyComplete()
    }
}
