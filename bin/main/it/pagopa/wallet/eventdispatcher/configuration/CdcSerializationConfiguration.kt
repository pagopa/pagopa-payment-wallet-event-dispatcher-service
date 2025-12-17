package it.pagopa.wallet.eventdispatcher.configuration

import com.azure.core.serializer.json.jackson.JacksonJsonSerializerBuilder
import com.azure.core.util.serializer.JsonSerializerProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import it.pagopa.wallet.eventdispatcher.common.cdc.LoggingEvent
import it.pagopa.wallet.eventdispatcher.common.serialization.CdcWalletEventMixin
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class CdcSerializationConfiguration {

    @Bean("cdcObjectMapperBuilder")
    fun cdcObjectMapperBuilder(): Jackson2ObjectMapperBuilder =
        Jackson2ObjectMapperBuilder()
            .modules(Jdk8Module(), JavaTimeModule(), kotlinModule())
            .mixIn(LoggingEvent::class.java, CdcWalletEventMixin::class.java)

    @Bean("cdcObjectMapper")
    fun cdcObjectMapper(
        @Qualifier("cdcObjectMapperBuilder") cdcObjectMapperBuilder: Jackson2ObjectMapperBuilder
    ): ObjectMapper = cdcObjectMapperBuilder.build()

    @Bean("cdcAzureJsonSerializer")
    fun cdcAzureJsonSerializer(
        @Qualifier("cdcObjectMapper") cdcObjectMapper: ObjectMapper
    ): JsonSerializerProvider = JsonSerializerProvider {
        JacksonJsonSerializerBuilder().serializer(cdcObjectMapper).build()
    }
}
