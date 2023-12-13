package no.nav.helse.flex.syketilfelle.kafka

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.common.serialization.Serializer

class JacksonKafkaSerializer<T : Any> : Serializer<T> {
    val objectMapper: ObjectMapper =
        ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }.also {
            it.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }

    override fun serialize(
        topic: String?,
        data: T?,
    ): ByteArray = objectMapper.writeValueAsBytes(data)
}
