package it.pagopa.wallet.eventdispatcher.configuration

import it.pagopa.wallet.eventdispatcher.configuration.properties.WalletCDCConfiguration
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import reactor.kafka.sender.SenderOptions

@Configuration
class WalletCDCClientConfiguration(private val environment: Environment) {

    @Bean(name = ["cdcEventHubClient"])
    fun cdcKafkaTemplate(
        walletCDCConfiguration: WalletCDCConfiguration
    ): ReactiveKafkaProducerTemplate<String, Any> {
        val configProps =
            mutableMapOf<String, Any>(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to walletCDCConfiguration.bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to
                    ErrorHandlingDeserializer::class.java
            )

        // Local testing in Docker do not require SASL
        if (!environment.activeProfiles.contains("local")) {
            configProps[SaslConfigs.SASL_JAAS_CONFIG] =
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"\$ConnectionString\" password=\"${walletCDCConfiguration.connectionString}\";"
            configProps[SaslConfigs.SASL_MECHANISM] = "PLAIN"
            configProps[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
        }
        val senderOptions = SenderOptions.create<String, Any>(configProps)
        return ReactiveKafkaProducerTemplate(senderOptions)
    }

    @Bean
    fun cdcTopicName(walletCDCConfiguration: WalletCDCConfiguration): String {
        return walletCDCConfiguration.cdcTopicName
    }
}
