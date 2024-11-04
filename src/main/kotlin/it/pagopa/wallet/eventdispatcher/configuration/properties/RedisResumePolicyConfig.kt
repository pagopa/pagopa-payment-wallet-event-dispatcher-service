package it.pagopa.wallet.eventdispatcher.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cdc.redis-resume")
data class RedisResumePolicyConfig(
    val keyspace: String,
    val target: String,
    val fallbackInMin: Long,
    val ttlInMin: Long
)
