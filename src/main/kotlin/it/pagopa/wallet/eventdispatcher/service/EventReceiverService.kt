package it.pagopa.wallet.eventdispatcher.service

import it.pagopa.generated.paymentwallet.eventdispatcher.server.model.*
import it.pagopa.wallet.eventdispatcher.configuration.properties.RedisStreamEventControllerConfigs
import it.pagopa.wallet.eventdispatcher.configuration.redis.EventDispatcherCommandsTemplateWrapper
import it.pagopa.wallet.eventdispatcher.configuration.redis.EventDispatcherReceiverStatusTemplateWrapper
import it.pagopa.wallet.eventdispatcher.exceptions.NoEventReceiverStatusFound
import it.pagopa.wallet.eventdispatcher.streams.commands.EventDispatcherReceiverCommand
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/** This class handles all InboundChannelsAdapters events receivers */
@Service
class EventReceiverService(
    @Autowired
    private val eventDispatcherCommandsTemplateWrapper: EventDispatcherCommandsTemplateWrapper,
    @Autowired
    private val eventDispatcherReceiverStatusTemplateWrapper:
        EventDispatcherReceiverStatusTemplateWrapper,
    @Autowired private val redisStreamConf: RedisStreamEventControllerConfigs
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handleCommand(eventReceiverCommandRequestDto: EventReceiverCommandRequestDto) {
        val commandToSend =
            when (eventReceiverCommandRequestDto.command) {
                EventReceiverCommandRequestDto.Command.START ->
                    EventDispatcherReceiverCommand.ReceiverCommand.START
                EventReceiverCommandRequestDto.Command.STOP ->
                    EventDispatcherReceiverCommand.ReceiverCommand.STOP
            }
        logger.info("Received event receiver command request, command: {}", commandToSend)
        // trim all events before adding new event to be processed
        val recordId =
            eventDispatcherCommandsTemplateWrapper.writeEventToStreamTrimmingEvents(
                redisStreamConf.streamKey,
                EventDispatcherReceiverCommand(
                    receiverCommand = commandToSend,
                    version = eventReceiverCommandRequestDto.deploymentVersion
                ),
                0
            )

        logger.info("Sent new event to Redis stream with id: [{}]", recordId)
    }

    suspend fun getReceiversStatus(
        deploymentVersionDto: DeploymentVersionDto?
    ): EventReceiverStatusResponseDto {
        val lastStatuses =
            eventDispatcherReceiverStatusTemplateWrapper.allValuesInKeySpace()?.filter {
                if (deploymentVersionDto != null) {
                    it.version == deploymentVersionDto
                } else {
                    true
                }
            }
        if (lastStatuses.isNullOrEmpty()) {
            throw NoEventReceiverStatusFound()
        }
        val receiverStatuses =
            lastStatuses.map { receiverStatuses ->
                EventReceiverStatusDto(
                    receiverStatuses =
                        receiverStatuses.receiverStatuses.map { receiverStatus ->
                            ReceiverStatusDto(
                                status =
                                    receiverStatus.status.let {
                                        ReceiverStatusDto.Status.valueOf(it.toString())
                                    },
                                name = receiverStatus.name
                            )
                        },
                    instanceId = receiverStatuses.consumerInstanceId,
                    deploymentVersion = receiverStatuses.version
                )
            }

        return EventReceiverStatusResponseDto(status = receiverStatuses)
    }
}
