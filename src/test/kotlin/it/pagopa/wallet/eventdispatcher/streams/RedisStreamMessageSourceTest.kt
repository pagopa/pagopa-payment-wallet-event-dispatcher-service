package it.pagopa.wallet.eventdispatcher.streams

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.pagopa.generated.paymentwallet.eventdispatcher.server.model.DeploymentVersionDto
import it.pagopa.wallet.eventdispatcher.configuration.properties.RedisStreamEventControllerConfigs
import it.pagopa.wallet.eventdispatcher.configuration.redis.EventDispatcherCommandsTemplateWrapper
import it.pagopa.wallet.eventdispatcher.configuration.redis.stream.RedisStreamMessageSource
import it.pagopa.wallet.eventdispatcher.streams.commands.EventDispatcherCommandMixin
import it.pagopa.wallet.eventdispatcher.streams.commands.EventDispatcherGenericCommand
import it.pagopa.wallet.eventdispatcher.streams.commands.EventDispatcherReceiverCommand
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.hash.Jackson2HashMapper
import org.springframework.data.redis.stream.StreamReceiver
import reactor.core.publisher.Flux

class RedisStreamMessageSourceTest {

    private val streamReceiver: StreamReceiver<String, ObjectRecord<String, LinkedHashMap<*, *>>> =
        mock()
    private val eventDispatcherCommandsTemplateWrapper: EventDispatcherCommandsTemplateWrapper =
        mock()
    private val redisStreamConf =
        RedisStreamEventControllerConfigs(
            streamKey = "streamKey",
            consumerGroupPrefix = "consumerGroup",
            consumerNamePrefix = "consumerName",
            failOnErrorCreatingConsumerGroup = false
        )
    private val redisStreamMessageSource =
        RedisStreamMessageSource(
            redisStreamConf = redisStreamConf,
            redisStreamReceiver = streamReceiver,
            eventDispatcherCommandsTemplateWrapper = eventDispatcherCommandsTemplateWrapper
        )

    private val streamKey = redisStreamConf.streamKey
    private val consumerGroup = redisStreamConf.consumerGroup
    private val consumerName = redisStreamConf.consumerName

    private val objectMapper: ObjectMapper =
        jacksonObjectMapper()
            .addMixIn(
                EventDispatcherGenericCommand::class.java,
                EventDispatcherCommandMixin::class.java
            )

    @Test
    fun `Should create consumer group on initialization returning correct component type`() {
        // assertions
        // reset is needed since this test class is already initialized before test execution
        Mockito.reset(eventDispatcherCommandsTemplateWrapper)
        given(eventDispatcherCommandsTemplateWrapper.createGroup(streamKey, consumerGroup))
            .willReturn("OK")
        // test
        val localInstance =
            RedisStreamMessageSource(
                redisStreamConf = redisStreamConf,
                redisStreamReceiver = streamReceiver,
                eventDispatcherCommandsTemplateWrapper = eventDispatcherCommandsTemplateWrapper
            )
        verify(eventDispatcherCommandsTemplateWrapper, times(1))
            .createGroup(streamKey, consumerGroup, ReadOffset.from("0"))
        assertEquals("redis-stream:message-source", localInstance.componentType)
    }

    @Test
    fun `Should handle error creating consumer group on initialization without throw exception`() {
        // assertions
        // reset is needed since this test class is already initialized before test execution
        Mockito.reset(eventDispatcherCommandsTemplateWrapper)
        given(eventDispatcherCommandsTemplateWrapper.createGroup(streamKey, consumerGroup))
            .willThrow(RuntimeException("Error creating consumer group"))
        // test
        RedisStreamMessageSource(
            redisStreamConf = redisStreamConf,
            redisStreamReceiver = streamReceiver,
            eventDispatcherCommandsTemplateWrapper = eventDispatcherCommandsTemplateWrapper
        )
        verify(eventDispatcherCommandsTemplateWrapper, times(1))
            .createGroup(streamKey, consumerGroup, ReadOffset.from("0"))
    }

    @Test
    fun `Should handle error creating consumer group on initialization throwing exception`() {
        // assertions
        // reset is needed since this test class is already initialized before test execution
        Mockito.reset(eventDispatcherCommandsTemplateWrapper)
        given(eventDispatcherCommandsTemplateWrapper.createGroup(any(), any(), any()))
            .willThrow(RuntimeException("Error creating consumer group"))
        // test
        assertThrows<IllegalStateException> {
            RedisStreamMessageSource(
                redisStreamConf =
                    RedisStreamEventControllerConfigs(
                        streamKey = "streamKey",
                        consumerGroupPrefix = "consumerGroupPrefix",
                        consumerNamePrefix = "consumerName",
                        failOnErrorCreatingConsumerGroup = true
                    ),
                redisStreamReceiver = streamReceiver,
                eventDispatcherCommandsTemplateWrapper = eventDispatcherCommandsTemplateWrapper
            )
        }

        verify(eventDispatcherCommandsTemplateWrapper, times(1)).createGroup(any(), any(), any())
    }

    @Test
    fun `Should receive event from stream successfully`() {
        // assertions
        val expectedCommand =
            EventDispatcherReceiverCommand(
                receiverCommand = EventDispatcherReceiverCommand.ReceiverCommand.START,
                version = DeploymentVersionDto.PROD
            )
        val hashMapSerializedObject =
            LinkedHashMap(Jackson2HashMapper(objectMapper, true).toHash(expectedCommand))
                as LinkedHashMap<*, *>
        val objectRecord = ObjectRecord.create(streamKey, hashMapSerializedObject)
        val retrievedEventsFromStream = Flux.fromIterable(listOf(objectRecord))
        given(
                streamReceiver.receiveAutoAck(
                    Consumer.from(consumerGroup, consumerName),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                )
            )
            .willReturn(retrievedEventsFromStream)
        // test
        val message = redisStreamMessageSource.doReceive()
        // verifications
        assertNotNull(message) // <----
        val messageHeaders = message!!.headers
        val event = message.payload
        assertEquals(objectRecord.id.value, messageHeaders[RedisStreamMessageSource.REDIS_EVENT_ID])
        assertEquals(
            objectRecord.id.timestamp,
            messageHeaders[RedisStreamMessageSource.REDIS_EVENT_TIMESTAMP]
        )
        assertEquals(streamKey, messageHeaders[RedisStreamMessageSource.REDIS_EVENT_STREAM_KEY])
        assertEquals(expectedCommand, event)
        verify(streamReceiver, times(1)).receiveAutoAck(any(), any())
    }

    @Test
    fun `Should handle error receiving event from stream`() {
        // assertions
        given(
                streamReceiver.receiveAutoAck(
                    Consumer.from(consumerGroup, consumerName),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                )
            )
            .willReturn(Flux.error(RuntimeException("Error polling for records")))
        // test
        val message = redisStreamMessageSource.doReceive()
        // verifications
        assertNull(message)
        verify(streamReceiver, times(1)).receiveAutoAck(any(), any())
    }
}
