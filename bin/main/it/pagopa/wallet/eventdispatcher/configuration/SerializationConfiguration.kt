package it.pagopa.wallet.eventdispatcher.configuration

import com.azure.core.serializer.json.jackson.JacksonJsonSerializerBuilder
import com.azure.core.util.serializer.JsonSerializerProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import it.pagopa.wallet.eventdispatcher.common.serialization.WalletEventMixin
import it.pagopa.wallet.eventdispatcher.domain.WalletEvent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class SerializationConfiguration {

    @Bean("objectMapperBuilder")
    fun objectMapperBuilder(): Jackson2ObjectMapperBuilder =
        Jackson2ObjectMapperBuilder()
            .modules(Jdk8Module(), JavaTimeModule(), kotlinModule())
            .mixIn(WalletEvent::class.java, WalletEventMixin::class.java)

    @Bean("objectMapper")
    fun objectMapper(
        @Qualifier("objectMapperBuilder") objectMapperBuilder: Jackson2ObjectMapperBuilder
    ): ObjectMapper = objectMapperBuilder.build()

    @Bean("azureJsonSerializer")
    fun azureJsonSerializer(
        @Qualifier("objectMapper") objectMapper: ObjectMapper
    ): JsonSerializerProvider = JsonSerializerProvider {
        JacksonJsonSerializerBuilder().serializer(objectMapper).build()
    }
}
