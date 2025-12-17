package it.pagopa.wallet.eventdispatcher.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cdc.retry-send")
data class RetrySendPolicyConfig(val maxAttempts: Long, val intervalInMs: Long) {}
