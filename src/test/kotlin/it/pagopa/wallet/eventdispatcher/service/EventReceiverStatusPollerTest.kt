package it.pagopa.wallet.eventdispatcher.service

import it.pagopa.generated.paymentwallet.eventdispatcher.server.model.DeploymentVersionDto
import it.pagopa.wallet.eventdispatcher.configuration.properties.RedisStreamEventControllerConfigs
import it.pagopa.wallet.eventdispatcher.configuration.redis.EventDispatcherReceiverStatusTemplateWrapper
import it.pagopa.wallet.eventdispatcher.repositories.redis.bean.ReceiverStatus
import it.pagopa.wallet.eventdispatcher.repositories.redis.bean.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class EventReceiverStatusPollerTest {

    private val inboundChannelAdapterLifecycleHandlerService:
        InboundChannelAdapterLifecycleHandlerService =
        mock()

    private val eventDispatcherReceiverStatusTemplateWrapper:
        EventDispatcherReceiverStatusTemplateWrapper =
        mock()

    private val redisStreamEventControllerConfigs =
        RedisStreamEventControllerConfigs(
            streamKey = "streamKey",
            consumerNamePrefix = "consumerName"
        )

    private val eventReceiverStatusPoller =
        EventReceiverStatusPoller(
            inboundChannelAdapterLifecycleHandlerService =
                inboundChannelAdapterLifecycleHandlerService,
            redisStreamEventControllerConfigs = redisStreamEventControllerConfigs,
            eventDispatcherReceiverStatusTemplateWrapper =
                eventDispatcherReceiverStatusTemplateWrapper,
            deploymentVersion = DeploymentVersionDto.PROD
        )

    @Test
    fun `Should poll for status successfully saving receiver statuses`() {
        // assertions
        val receiverStatuses =
            listOf(
                ReceiverStatus(name = "receiver1", status = Status.UP),
                ReceiverStatus(name = "receiver2", status = Status.DOWN)
            )
        given(inboundChannelAdapterLifecycleHandlerService.getAllChannelStatus())
            .willReturn(receiverStatuses)
        doNothing().`when`(eventDispatcherReceiverStatusTemplateWrapper).save(any(), any())
        // test
        eventReceiverStatusPoller.eventReceiverStatusPoller()
        verify(eventDispatcherReceiverStatusTemplateWrapper, times(1))
            .save(
                argThat {
                    assertEquals(
                        redisStreamEventControllerConfigs.consumerName,
                        this.consumerInstanceId
                    )
                    assertEquals(receiverStatuses, this.receiverStatuses)
                    true
                },
                anyOrNull()
            )
    }
}
