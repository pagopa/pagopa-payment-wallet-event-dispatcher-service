package it.pagopa.wallet.eventdispatcher.configuration.properties

import java.util.*
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "redis-stream.event-controller")
class RedisStreamEventControllerConfigs(
    val streamKey: String,
    consumerNamePrefix: String,
    consumerGroupPrefix: String,
    val failOnErrorCreatingConsumerGroup: Boolean
) {
    private val uniqueConsumerId = UUID.randomUUID().toString()
    val consumerName = "$consumerNamePrefix-$uniqueConsumerId"
    val consumerGroup = "$consumerGroupPrefix-$uniqueConsumerId"
}
