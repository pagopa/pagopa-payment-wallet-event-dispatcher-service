package it.pagopa.wallet.eventdispatcher.streams

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.pagopa.generated.paymentwallet.eventdispatcher.server.model.DeploymentVersionDto
import it.pagopa.wallet.eventdispatcher.configuration.properties.RedisStreamEventControllerConfigs
import it.pagopa.wallet.eventdispatcher.service.InboundChannelAdapterLifecycleHandlerService
import it.pagopa.wallet.eventdispatcher.streams.commands.EventDispatcherGenericCommand
import it.pagopa.wallet.eventdispatcher.streams.commands.EventDispatcherReceiverCommand
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.stream.StreamReceiver
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

/**
 * Redis Stream event consumer. This class handles all Redis Stream events performing requested
 * operation based on input event type
 */
@Service
class RedisStreamConsumer(
    @Autowired
    private val inboundChannelAdapterLifecycleHandlerService:
        InboundChannelAdapterLifecycleHandlerService,
    @Value("\${eventController.deploymentVersion}")
    private val deploymentVersion: DeploymentVersionDto,
    @Autowired
    private val redisStreamReceiver:
        StreamReceiver<String, ObjectRecord<String, LinkedHashMap<*, *>>>,
    @Autowired private val redisStreamConf: RedisStreamEventControllerConfigs
) {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val logger = LoggerFactory.getLogger(javaClass)

    @ServiceActivator(
        inputChannel = "eventDispatcherReceiverCommandChannel",
        outputChannel = "nullChannel"
    )
    fun readStreamEvent(@Payload message: Message<EventDispatcherGenericCommand>) {
        logger.info("Received event: {}", message)
        when (val command = message.payload) {
            is EventDispatcherReceiverCommand -> handleEventReceiverCommand(command)
            else -> throw RuntimeException("Unhandled command received: $command")
        }
    }

    /** Handle event receiver command to start/stop receivers */
    fun handleEventReceiverCommand(command: EventDispatcherReceiverCommand) {
        // current deployment version is targeted by command for exact version match or if command
        // does not explicit a targeted version
        val currentDeploymentVersion = deploymentVersion
        val commandTargetVersion = command.version
        val isTargetedByCommand =
            commandTargetVersion == null || currentDeploymentVersion == commandTargetVersion
        logger.info(
            "Event dispatcher receiver command event received. Current deployment version: [{}], command deployment version: [{}] -> is this version targeted: [{}]",
            currentDeploymentVersion,
            commandTargetVersion ?: "ALL",
            isTargetedByCommand
        )
        if (isTargetedByCommand) {
            val commandToSend =
                when (command.receiverCommand) {
                    EventDispatcherReceiverCommand.ReceiverCommand.START -> "start"
                    EventDispatcherReceiverCommand.ReceiverCommand.STOP -> "stop"
                }
            inboundChannelAdapterLifecycleHandlerService.invokeCommandForAllEndpoints(commandToSend)
        } else {
            logger.info(
                "Current deployment version not targeted by command, command will not be processed"
            )
        }
    }
}
