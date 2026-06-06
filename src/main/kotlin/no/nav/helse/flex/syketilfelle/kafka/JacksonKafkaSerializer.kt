package no.nav.helse.flex.syketilfelle.kafka

import com.fasterxml.jackson.annotation.JsonInclude
import org.apache.kafka.common.serialization.Serializer
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

class JacksonKafkaSerializer<T> : Serializer<T> {
    private val objectMapper: ObjectMapper =
        JsonMapper
            .builder()
            .addModule(kotlinModule())
            .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
            .build()

    override fun serialize(
        topic: String?,
        data: T?,
    ): ByteArray? = data?.let { objectMapper.writeValueAsBytes(it) }
}
