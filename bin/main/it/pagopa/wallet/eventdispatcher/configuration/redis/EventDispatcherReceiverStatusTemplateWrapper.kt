package it.pagopa.wallet.eventdispatcher.configuration.redis

import it.pagopa.wallet.eventdispatcher.repositories.redis.bean.ReceiversStatus
import it.pagopa.wallet.eventdispatcher.utils.ReactiveRedisTemplateWrapper
import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate

/** Redis template wrapper used to handle event receiver statuses */
class EventDispatcherReceiverStatusTemplateWrapper(
    redisTemplate: ReactiveRedisTemplate<String, ReceiversStatus>,
    defaultEntitiesTTL: Duration
) :
    ReactiveRedisTemplateWrapper<ReceiversStatus>(
        redisTemplate,
        "receiver-status",
        defaultEntitiesTTL
    ) {
    override fun getKeyFromEntity(value: ReceiversStatus): String = value.consumerInstanceId
}
