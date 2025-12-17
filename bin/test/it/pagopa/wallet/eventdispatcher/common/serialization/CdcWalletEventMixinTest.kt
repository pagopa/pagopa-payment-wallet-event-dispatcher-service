package it.pagopa.wallet.eventdispatcher.common.serialization

import com.azure.core.util.BinaryData
import com.azure.core.util.serializer.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.wallet.eventdispatcher.common.cdc.*
import it.pagopa.wallet.eventdispatcher.common.queue.CdcQueueEvent
import it.pagopa.wallet.eventdispatcher.common.queue.TracingInfo
import it.pagopa.wallet.eventdispatcher.configuration.CdcSerializationConfiguration
import java.nio.charset.StandardCharsets
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import reactor.kotlin.test.test

class CdcWalletEventMixinTest {

    companion object {
        private val mockedTracingInfo =
            TracingInfo(baggage = "baggage", tracestate = "tracestate", traceparent = "traceparent")

        @JvmStatic
        fun defaultEventMethodSource(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    """
                    {
                        "data": {
                            "_id": "bcfb7296-c53f-4840-9977-84a597fca1a0",
                            "timestamp": "2024-06-12T15:50:47.231210Z",
                            "walletId": "a21e0037-251d-413b-b121-8899e368df7e",
                            "_class": "it.pagopa.wallet.audit.WalletAddedEvent"
                        },
                        "tracingInfo": {
                            "traceparent": "traceparent",
                            "tracestate": "tracestate",
                            "baggage": "baggage"
                        }
                    }
                """,
                    CdcQueueEvent(
                        tracingInfo = mockedTracingInfo,
                        data =
                            LoggingEvent(
                                "bcfb7296-c53f-4840-9977-84a597fca1a0",
                                "2024-06-12T15:50:47.231210Z"
                            )
                    )
                ),
                Arguments.of(
                    """
                    {
                        "data": {
                            "_id": "bcfb7296-c53f-4840-9977-84a597fca1a0",
                            "serviceId": "PAGOPA",
                            "oldStatus" : "ENABLED",
                            "newStatus" : "DISABLED",
                            "timestamp": "2024-06-12T15:50:47.231210Z",
                            "_class": "it.pagopa.wallet.audit.ApplicationStatusChangedEvent"
                        },
                        "tracingInfo": {
                            "traceparent": "traceparent",
                            "tracestate": "tracestate",
                            "baggage": "baggage"
                        }
                    }
                """,
                    CdcQueueEvent(
                        tracingInfo = mockedTracingInfo,
                        data =
                            LoggingEvent(
                                "bcfb7296-c53f-4840-9977-84a597fca1a0",
                                "2024-06-12T15:50:47.231210Z"
                            )
                    )
                )
            )

        @JvmStatic
        fun roundTripEventMethodSource(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    """
                    {
                        "data": {
                            "_id": "bcfb7296-c53f-4840-9977-84a597fca1a0",
                            "timestamp": "2024-06-12T15:50:47.231210Z",
                            "walletId": "a21e0037-251d-413b-b121-8899e368df7e",
                            "_class": "it.pagopa.wallet.audit.WalletDeletedEvent"
                        },
                        "tracingInfo": {
                            "traceparent": "traceparent",
                            "tracestate": "tracestate",
                            "baggage": "baggage"
                        }
                    }
                """,
                    CdcQueueEvent(
                        tracingInfo = mockedTracingInfo,
                        data =
                            WalletDeletedEvent(
                                id = "bcfb7296-c53f-4840-9977-84a597fca1a0",
                                timestamp = "2024-06-12T15:50:47.231210Z",
                                walletId = "a21e0037-251d-413b-b121-8899e368df7e"
                            )
                    )
                ),
                Arguments.of(
                    """
                    {
                        "data":{
                          "_id":"d283cbc5-cc48-4bc4-8f00-ddba4e24fc91",
                          "walletId":"a527e843-9d1c-4531-ae5b-3809cc7abe7a",
                          "auditWallet":{
                             "paymentMethodId":"9d735400-9450-4f7e-9431-8c1e7fa2a339",
                             "creationDate":"2024-10-16T15:03:18.541220633Z",
                             "updateDate":"2024-10-16T15:03:36.447051359Z",
                             "applications":[
                                {
                                   "_id":"PAGOPA",
                                   "status":"ENABLED",
                                   "creationDate":"2024-10-16T15:03:18.378746985Z",
                                   "updateDate":"2024-10-16T15:03:18.378747385Z",
                                   "metadata":{
                                      
                                   }
                                }
                             ],
                             "details":{
                                "type":"PAYPAL",
                                "pspId":"BCITITMM"
                             },
                             "status":"VALIDATED",
                             "validationOperationId":"618534471407042909",
                             "validationOperationResult":"EXECUTED",
                             "validationOperationTimestamp":"2024-10-16T15:03:35.841Z"
                          },
                          "timestamp":"2024-10-16T15:03:36.527818530Z",
                          "_class":"it.pagopa.wallet.audit.WalletOnboardCompletedEvent"
                       },
                       "tracingInfo":{
                          
                       }
                    }

                """,
                    CdcQueueEvent(
                        tracingInfo = TracingInfo(),
                        data =
                            WalletOnboardCompletedEvent(
                                id = "d283cbc5-cc48-4bc4-8f00-ddba4e24fc91",
                                timestamp = "2024-10-16T15:03:36.527818530Z",
                                walletId = "a527e843-9d1c-4531-ae5b-3809cc7abe7a",
                                auditWallet =
                                    AuditWallet(
                                        paymentMethodId = "9d735400-9450-4f7e-9431-8c1e7fa2a339",
                                        creationDate = "2024-10-16T15:03:18.541220633Z",
                                        updateDate = "2024-10-16T15:03:36.447051359Z",
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
                    ),
                    Arguments.of(
                        """
                    {
                        "data":{
                          "_id":"d283cbc5-cc48-4bc4-8f00-ddba4e24fc91",
                          "walletId":"a527e843-9d1c-4531-ae5b-3809cc7abe7a",
                          "auditWallet":{
                             "paymentMethodId":"9d735400-9450-4f7e-9431-8c1e7fa2a339",
                             "creationDate":"2024-10-16T15:03:18.541220633Z",
                             "updateDate":"2024-10-16T15:03:36.447051359Z",
                             "applications":[
                                {
                                   "_id":"PAGOPA",
                                   "status":"ENABLED",
                                   "creationDate":"2024-10-16T15:03:18.378746985Z",
                                   "updateDate":"2024-10-16T15:03:18.378747385Z",
                                   "metadata":{
                                      
                                   }
                                }
                             ],
                             "details":{
                                "type":"CARDS",
                                "cardBrand":"VISA"
                             },
                             "status":"VALIDATED",
                             "validationOperationId":"618534471407042909",
                             "validationOperationResult":"EXECUTED",
                             "validationOperationTimestamp":"2024-10-16T15:03:35.841Z"
                          },
                          "timestamp":"2024-10-16T15:03:36.527818530Z",
                          "_class":"it.pagopa.wallet.audit.WalletOnboardCompletedEvent"
                       },
                       "tracingInfo":{
                          
                       }
                    }

                """,
                        CdcQueueEvent(
                            tracingInfo = TracingInfo(),
                            data =
                                WalletOnboardCompletedEvent(
                                    id = "d283cbc5-cc48-4bc4-8f00-ddba4e24fc91",
                                    timestamp = "2024-10-16T15:03:36.527818530Z",
                                    walletId = "a527e843-9d1c-4531-ae5b-3809cc7abe7a",
                                    auditWallet =
                                        AuditWallet(
                                            paymentMethodId =
                                                "9d735400-9450-4f7e-9431-8c1e7fa2a339",
                                            creationDate = "2024-10-16T15:03:18.541220633Z",
                                            updateDate = "2024-10-16T15:03:36.447051359Z",
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
                                            details = AuditWalletDetails("CARDS", "VISA", null),
                                            status = "VALIDATED",
                                            validationOperationId = "618534471407042909",
                                            validationOperationResult = "EXECUTED",
                                            validationOperationTimestamp =
                                                "2024-10-16T15:03:35.841Z",
                                            validationErrorCode = null
                                        )
                                )
                        )
                    )
                )
            )
    }

    private val serializationConfiguration = CdcSerializationConfiguration()
    private val objectMapperBuilder = serializationConfiguration.cdcObjectMapperBuilder()
    private val objectMapper: ObjectMapper =
        serializationConfiguration.cdcObjectMapper(objectMapperBuilder)
    private val azureJsonSerializer =
        serializationConfiguration.cdcAzureJsonSerializer(objectMapper).createInstance()

    @ParameterizedTest
    @MethodSource("roundTripEventMethodSource")
    fun `Can round trip events successfully`(
        serializedEvent: String,
        expectedDeserializedEvent: CdcQueueEvent<LoggingEvent>
    ) {
        BinaryData.fromBytes(serializedEvent.toByteArray(StandardCharsets.UTF_8))
            .toObjectAsync(
                object : TypeReference<CdcQueueEvent<LoggingEvent>>() {},
                azureJsonSerializer
            )
            .test()
            .consumeNextWith { assertEquals(expectedDeserializedEvent, it) }
            .verifyComplete()
    }

    @ParameterizedTest
    @MethodSource("defaultEventMethodSource")
    fun `Can default to LoggingEvent successfully`(
        serializedEvent: String,
        expectedDeserializedEvent: CdcQueueEvent<LoggingEvent>
    ) {
        BinaryData.fromBytes(serializedEvent.toByteArray(StandardCharsets.UTF_8))
            .toObjectAsync(
                object : TypeReference<CdcQueueEvent<LoggingEvent>>() {},
                azureJsonSerializer
            )
            .test()
            .consumeNextWith { assertTrue(it.data::class == LoggingEvent::class) }
            .verifyComplete()
    }
}
