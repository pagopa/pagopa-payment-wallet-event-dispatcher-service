package it.pagopa.wallet.eventdispatcher.configuration.properties

import java.util.*
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "redis-stream.event-controller")
class RedisStreamEventControllerConfigs(
    val streamKey: String,
    consumerNamePrefix: String,
) {
    private val uniqueConsumerId = UUID.randomUUID().toString()
    val consumerName = "$consumerNamePrefix-$uniqueConsumerId"
}
