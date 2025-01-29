package it.pagopa.wallet.eventdispatcher.service

import it.pagopa.generated.paymentwallet.eventdispatcher.server.model.*
import it.pagopa.wallet.eventdispatcher.configuration.properties.RedisStreamEventControllerConfigs
import it.pagopa.wallet.eventdispatcher.configuration.redis.EventDispatcherCommandsTemplateWrapper
import it.pagopa.wallet.eventdispatcher.configuration.redis.EventDispatcherReceiverStatusTemplateWrapper
import it.pagopa.wallet.eventdispatcher.exceptions.NoEventReceiverStatusFound
import it.pagopa.wallet.eventdispatcher.repositories.redis.bean.ReceiverStatus
import it.pagopa.wallet.eventdispatcher.repositories.redis.bean.ReceiversStatus
import it.pagopa.wallet.eventdispatcher.repositories.redis.bean.Status
import it.pagopa.wallet.eventdispatcher.streams.commands.EventDispatcherReceiverCommand
import java.time.OffsetDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.*
import org.springframework.data.redis.connection.stream.RecordId

@OptIn(ExperimentalCoroutinesApi::class)
class EventReceiverServiceTest {

    private val eventDispatcherCommandsTemplateWrapper: EventDispatcherCommandsTemplateWrapper =
        mock()
    private val eventDispatcherReceiverStatusTemplateWrapper:
        EventDispatcherReceiverStatusTemplateWrapper =
        mock()
    private val redisStreamConf =
        RedisStreamEventControllerConfigs(
            streamKey = "streamKey",
            consumerNamePrefix = "consumerNamePrefix"
        )

    private val eventReceiverService =
        EventReceiverService(
            eventDispatcherReceiverStatusTemplateWrapper =
                eventDispatcherReceiverStatusTemplateWrapper,
            redisStreamConf = redisStreamConf,
            eventDispatcherCommandsTemplateWrapper = eventDispatcherCommandsTemplateWrapper
        )

    @ParameterizedTest
    @EnumSource(EventReceiverCommandRequestDto.Command::class)
    fun `Should handle command correctly`(requestCommand: EventReceiverCommandRequestDto.Command) =
        runTest {
            // pre-requisites
            val deploymentVersion = DeploymentVersionDto.PROD
            val request =
                EventReceiverCommandRequestDto(
                    command = requestCommand,
                    deploymentVersion = deploymentVersion
                )
            val expectedCommand =
                EventDispatcherReceiverCommand(
                    receiverCommand =
                        EventDispatcherReceiverCommand.ReceiverCommand.valueOf(
                            requestCommand.toString()
                        ),
                    version = deploymentVersion
                )
            given(
                    eventDispatcherCommandsTemplateWrapper.writeEventToStreamTrimmingEvents(
                        any(),
                        any(),
                        any()
                    )
                )
                .willReturn(RecordId.autoGenerate())
            // test
            eventReceiverService.handleCommand(request)
            // assertions
            verify(eventDispatcherCommandsTemplateWrapper, times(1))
                .writeEventToStreamTrimmingEvents(redisStreamConf.streamKey, expectedCommand, 0)
        }

    @Test
    fun `Should retrieve receiver statuses successfully for all deployment versions`() = runTest {
        // pre-requisites
        val receiverStatuses =
            listOf(
                ReceiversStatus(
                    receiverStatuses =
                        listOf(ReceiverStatus(name = "receiverName1", status = Status.DOWN)),
                    version = DeploymentVersionDto.PROD,
                    queriedAt = OffsetDateTime.now().toString(),
                    consumerInstanceId = "consumerInstanceId1"
                ),
                ReceiversStatus(
                    receiverStatuses =
                        listOf(ReceiverStatus(name = "receiverName2", status = Status.UP)),
                    version = DeploymentVersionDto.STAGING,
                    queriedAt = OffsetDateTime.now().toString(),
                    consumerInstanceId = "consumerInstanceId2"
                )
            )
        val expectedResponse =
            EventReceiverStatusResponseDto(
                status =
                    listOf(
                        EventReceiverStatusDto(
                            instanceId = "consumerInstanceId1",
                            receiverStatuses =
                                listOf(
                                    ReceiverStatusDto(
                                        name = "receiverName1",
                                        status = ReceiverStatusDto.Status.DOWN
                                    )
                                ),
                            deploymentVersion = DeploymentVersionDto.PROD
                        ),
                        EventReceiverStatusDto(
                            instanceId = "consumerInstanceId2",
                            receiverStatuses =
                                listOf(
                                    ReceiverStatusDto(
                                        name = "receiverName2",
                                        status = ReceiverStatusDto.Status.UP
                                    )
                                ),
                            deploymentVersion = DeploymentVersionDto.STAGING
                        )
                    )
            )
        given(eventDispatcherReceiverStatusTemplateWrapper.allValuesInKeySpace())
            .willReturn(receiverStatuses.toMutableList())

        // test
        val response = eventReceiverService.getReceiversStatus(deploymentVersionDto = null)
        // assertions
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `Should retrieve receiver statuses successfully filtering for deployment version`() =
        runTest {
            // pre-requisites
            val receiverStatuses =
                listOf(
                    ReceiversStatus(
                        receiverStatuses =
                            listOf(ReceiverStatus(name = "receiverName1", status = Status.DOWN)),
                        version = DeploymentVersionDto.PROD,
                        queriedAt = OffsetDateTime.now().toString(),
                        consumerInstanceId = "consumerInstanceId1"
                    ),
                    ReceiversStatus(
                        receiverStatuses =
                            listOf(ReceiverStatus(name = "receiverName2", status = Status.UP)),
                        version = DeploymentVersionDto.STAGING,
                        queriedAt = OffsetDateTime.now().toString(),
                        consumerInstanceId = "consumerInstanceId2"
                    )
                )
            val expectedResponse =
                EventReceiverStatusResponseDto(
                    status =
                        listOf(
                            EventReceiverStatusDto(
                                instanceId = "consumerInstanceId1",
                                receiverStatuses =
                                    listOf(
                                        ReceiverStatusDto(
                                            name = "receiverName1",
                                            status = ReceiverStatusDto.Status.DOWN
                                        )
                                    ),
                                deploymentVersion = DeploymentVersionDto.PROD
                            )
                        )
                )
            given(eventDispatcherReceiverStatusTemplateWrapper.allValuesInKeySpace())
                .willReturn(receiverStatuses.toMutableList())

            // test
            val response =
                eventReceiverService.getReceiversStatus(
                    deploymentVersionDto = DeploymentVersionDto.PROD
                )
            // assertions
            assertEquals(expectedResponse, response)
        }

    @Test
    fun `Should throw NoEventReceiverStatusFound for no data found retrieving receiver statuses`() =
        runTest {
            // pre-requisites
            val receiverStatuses =
                listOf(
                    ReceiversStatus(
                        receiverStatuses =
                            listOf(ReceiverStatus(name = "receiverName1", status = Status.DOWN)),
                        version = DeploymentVersionDto.STAGING,
                        queriedAt = OffsetDateTime.now().toString(),
                        consumerInstanceId = "consumerInstanceId1"
                    ),
                    ReceiversStatus(
                        receiverStatuses =
                            listOf(ReceiverStatus(name = "receiverName2", status = Status.UP)),
                        version = DeploymentVersionDto.STAGING,
                        queriedAt = OffsetDateTime.now().toString(),
                        consumerInstanceId = "consumerInstanceId2"
                    )
                )
            val expectedResponse =
                EventReceiverStatusResponseDto(
                    status =
                        listOf(
                            EventReceiverStatusDto(
                                instanceId = "consumerInstanceId1",
                                receiverStatuses =
                                    listOf(
                                        ReceiverStatusDto(
                                            name = "receiverName1",
                                            status = ReceiverStatusDto.Status.DOWN
                                        )
                                    ),
                                deploymentVersion = DeploymentVersionDto.PROD
                            )
                        )
                )
            given(eventDispatcherReceiverStatusTemplateWrapper.allValuesInKeySpace())
                .willReturn(receiverStatuses.toMutableList())

            // test
            assertThrows<NoEventReceiverStatusFound> {
                eventReceiverService.getReceiversStatus(
                    deploymentVersionDto = DeploymentVersionDto.PROD
                )
            }
        }
}
