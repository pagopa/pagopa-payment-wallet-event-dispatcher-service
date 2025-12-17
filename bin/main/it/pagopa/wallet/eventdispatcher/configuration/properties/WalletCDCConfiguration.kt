package it.pagopa.wallet.eventdispatcher.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "azure.eventhub")
data class WalletCDCConfiguration(
    val bootstrapServers: String,
    val cdcTopicName: String,
    val connectionString: String
)
