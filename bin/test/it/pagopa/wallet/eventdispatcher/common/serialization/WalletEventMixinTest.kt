package it.pagopa.wallet.eventdispatcher.common.serialization

import com.azure.core.util.BinaryData
import com.azure.core.util.serializer.TypeReference
import it.pagopa.wallet.eventdispatcher.common.queue.QueueEvent
import it.pagopa.wallet.eventdispatcher.common.queue.TracingInfo
import it.pagopa.wallet.eventdispatcher.configuration.SerializationConfiguration
import it.pagopa.wallet.eventdispatcher.domain.WalletCreatedEvent
import it.pagopa.wallet.eventdispatcher.domain.WalletEvent
import it.pagopa.wallet.eventdispatcher.domain.WalletUsedEvent
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import reactor.kotlin.test.test

class WalletEventMixinTest {

    companion object {
        private val mockedTracingInfo =
            TracingInfo(baggage = "baggage", tracestate = "tracestate", traceparent = "traceparent")

        @JvmStatic
        fun roundTripEventMethodSource(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    """
                    {
                        "data": {
                            "type": "WalletCreated",
                            "eventId": "bcfb7296-c53f-4840-9977-84a597fca1a0",
                            "creationDate": "2024-06-12T15:50:47.231210Z",
                            "walletId": "a21e0037-251d-413b-b121-8899e368df7e"
                        },
                        "tracingInfo": {
                            "traceparent": "traceparent",
                            "tracestate": "tracestate",
                            "baggage": "baggage"
                        }
                    }
                """,
                    QueueEvent(
                        tracingInfo = mockedTracingInfo,
                        data =
                            WalletCreatedEvent(
                                walletId = "a21e0037-251d-413b-b121-8899e368df7e",
                                eventId = "bcfb7296-c53f-4840-9977-84a597fca1a0",
                                creationDate = OffsetDateTime.parse("2024-06-12T15:50:47.231210Z")
                            )
                    )
                ),
                Arguments.of(
                    """
                    {
                        "data": {
                            "type": "WalletUsed",
                            "eventId": "bcfb7296-c53f-4840-9977-84a597fca1a0",
                            "creationDate": "2024-06-12T15:50:47.231210Z",
                            "walletId": "a21e0037-251d-413b-b121-8899e368df7e",
                            "clientId": "IO"
                        },
                        "tracingInfo": {
                            "traceparent": "traceparent",
                            "tracestate": "tracestate",
                            "baggage": "baggage"
                        }
                    }
                """,
                    QueueEvent(
                        tracingInfo = mockedTracingInfo,
                        data =
                            WalletUsedEvent(
                                walletId = "a21e0037-251d-413b-b121-8899e368df7e",
                                eventId = "bcfb7296-c53f-4840-9977-84a597fca1a0",
                                creationDate = OffsetDateTime.parse("2024-06-12T15:50:47.231210Z"),
                                clientId = "IO"
                            )
                    )
                )
            )
    }

    private val serializationConfiguration = SerializationConfiguration()
    private val objectMapperBuilder: Jackson2ObjectMapperBuilder =
        serializationConfiguration.objectMapperBuilder()
    private val objectMapper = serializationConfiguration.objectMapper(objectMapperBuilder)
    private val azureJsonSerializer =
        serializationConfiguration.azureJsonSerializer(objectMapper).createInstance()

    @ParameterizedTest
    @MethodSource("roundTripEventMethodSource")
    fun `Can round trip events successfully`(
        serializedEvent: String,
        expectedDeserializedEvent: QueueEvent<WalletEvent>
    ) {
        BinaryData.fromBytes(serializedEvent.toByteArray(StandardCharsets.UTF_8))
            .toObjectAsync(
                object : TypeReference<QueueEvent<WalletEvent>>() {},
                azureJsonSerializer
            )
            .test()
            .consumeNextWith { assertEquals(expectedDeserializedEvent, it) }
            .verifyComplete()
    }
}
