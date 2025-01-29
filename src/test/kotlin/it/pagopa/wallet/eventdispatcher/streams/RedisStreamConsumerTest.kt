package it.pagopa.wallet.eventdispatcher.streams

import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.generated.paymentwallet.eventdispatcher.server.model.DeploymentVersionDto
import it.pagopa.wallet.eventdispatcher.configuration.properties.RedisStreamEventControllerConfigs
import it.pagopa.wallet.eventdispatcher.service.InboundChannelAdapterLifecycleHandlerService
import it.pagopa.wallet.eventdispatcher.streams.commands.EventDispatcherReceiverCommand
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.*
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.stream.StreamReceiver

class RedisStreamConsumerTest {
    private val inboundChannelAdapterLifecycleHandlerService:
        InboundChannelAdapterLifecycleHandlerService =
        mock()
    private val deploymentVersionDto = DeploymentVersionDto.PROD
    private val redisStreamReceiver:
        StreamReceiver<String, ObjectRecord<String, LinkedHashMap<*, *>>> =
        mock()
    private val redisStreamConf =
        RedisStreamEventControllerConfigs(
            streamKey = "streamKey",
            consumerNamePrefix = "consumerNamePrefix",
            consumerGroupPrefix = "consumerGroupPrefix",
            failOnErrorCreatingConsumerGroup = false
        )
    private val redisStreamConsumer =
        RedisStreamConsumer(
            deploymentVersion = deploymentVersionDto,
            redisStreamConf = redisStreamConf,
            redisStreamReceiver = redisStreamReceiver,
            inboundChannelAdapterLifecycleHandlerService =
                inboundChannelAdapterLifecycleHandlerService
        )

    private val streamKey = redisStreamConf.streamKey
    private val consumerGroup = redisStreamConf.consumerGroup
    private val consumerName = redisStreamConf.consumerName

    companion object {
        private val objectMapper = ObjectMapper()

        @JvmStatic
        fun `event dispatcher receiver command method source`(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    EventDispatcherReceiverCommand(
                        receiverCommand = EventDispatcherReceiverCommand.ReceiverCommand.START,
                        version = DeploymentVersionDto.PROD
                    ),
                    "start"
                ),
                Arguments.of(
                    EventDispatcherReceiverCommand(
                        receiverCommand = EventDispatcherReceiverCommand.ReceiverCommand.STOP,
                        version = DeploymentVersionDto.PROD
                    ),
                    "stop"
                ),
                Arguments.of(
                    EventDispatcherReceiverCommand(
                        receiverCommand = EventDispatcherReceiverCommand.ReceiverCommand.START,
                        version = null
                    ),
                    "start"
                ),
                Arguments.of(
                    EventDispatcherReceiverCommand(
                        receiverCommand = EventDispatcherReceiverCommand.ReceiverCommand.STOP,
                        version = null
                    ),
                    "stop"
                ),
            )
    }

    @ParameterizedTest
    @MethodSource("event dispatcher receiver command method source")
    fun `Should process event dispatcher receiver command from stream`(
        receivedEvent: EventDispatcherReceiverCommand,
        expectedCommandSend: String
    ) {
        // test
        redisStreamConsumer.handleEventReceiverCommand(receivedEvent)
        // verifications
        verify(inboundChannelAdapterLifecycleHandlerService, times(1))
            .invokeCommandForAllEndpoints(expectedCommandSend)
    }

    @Test
    fun `Should ignore events that does not target current deployment`() {
        val stagingEvent =
            EventDispatcherReceiverCommand(
                receiverCommand = EventDispatcherReceiverCommand.ReceiverCommand.STOP,
                version = DeploymentVersionDto.STAGING
            )
        // test
        redisStreamConsumer.handleEventReceiverCommand(stagingEvent)
        // verifications
        verify(inboundChannelAdapterLifecycleHandlerService, times(0))
            .invokeCommandForAllEndpoints(any())
    }
}
