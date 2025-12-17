package it.pagopa.wallet.eventdispatcher.configuration.redis

import it.pagopa.wallet.eventdispatcher.streams.commands.EventDispatcherReceiverCommand
import it.pagopa.wallet.eventdispatcher.utils.ReactiveRedisTemplateWrapper
import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate

/** Redis command template wrapper, used to write events to Redis stream */
class EventDispatcherCommandsTemplateWrapper(
    redisTemplate: ReactiveRedisTemplate<String, EventDispatcherReceiverCommand>,
    defaultEntitiesTTL: Duration
) :
    ReactiveRedisTemplateWrapper<EventDispatcherReceiverCommand>(
        redisTemplate,
        "eventDispatcher",
        defaultEntitiesTTL
    ) {
    public override fun getKeyFromEntity(value: EventDispatcherReceiverCommand): String =
        value.commandId.toString()
}
