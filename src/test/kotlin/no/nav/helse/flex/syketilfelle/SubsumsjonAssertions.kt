package no.nav.helse.flex.syketilfelle

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.URI

internal object SubsumsjonAssertions {
    private val objectMapper = jacksonObjectMapper()

    private val schema: Schema by lazy {
        val schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7)
        val url = URI("https://raw.githubusercontent.com/navikt/helse/main/subsumsjon/json-schema-1.0.0.json").toURL()
        schemaRegistry.getSchema(
            url.openStream().use { objectMapper.readTree(it) },
        )
    }

    internal fun assertSubsumsjonsmelding(melding: JsonNode) {
        assertEquals(emptyList<com.networknt.schema.Error>(), schema.validate(melding))
    }

    internal fun assertSubsumsjonsmelding(melding: String) {
        assertSubsumsjonsmelding(objectMapper.readTree(melding))
    }
}
