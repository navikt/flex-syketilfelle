package no.nav.helse.flex.syketilfelle

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.SubsumsjonAssertions.assertSubsumsjonsmelding
import no.nav.helse.flex.syketilfelle.juridiskvurdering.JuridiskVurderingKafkaDto
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.awaitility.Awaitility.await
import java.time.Duration

fun <K, V> Consumer<K, V>.subscribeHvisIkkeSubscribed(vararg topics: String) {
    if (this.subscription().isEmpty()) {
        this.subscribe(listOf(*topics))
    }
}

fun <K, V> Consumer<K, V>.hentProduserteRecords(duration: Duration = Duration.ofMillis(100)): List<ConsumerRecord<K, V>> =
    this
        .poll(duration)
        .also {
            this.commitSync()
        }.iterator()
        .asSequence()
        .toList()

fun <K, V> Consumer<K, V>.ventPåRecords(
    antall: Int,
    duration: Duration = Duration.ofMillis(1000),
): List<ConsumerRecord<K, V>> {
    val factory =
        if (antall == 0) {
            // Må vente fullt ut, ikke opp til en tid siden vi vil se at ingen blir produsert
            await().during(duration)
        } else {
            await().atMost(duration)
        }

    val alle = ArrayList<ConsumerRecord<K, V>>()
    factory.until {
        alle.addAll(this.hentProduserteRecords())
        alle.size == antall
    }
    return alle
}

fun List<ConsumerRecord<String, String>>.tilJuridiskVurdering(): List<JuridiskVurderingKafkaDto> =
    this
        .map {
            assertSubsumsjonsmelding(it.value())
            it.value()
        }.map { objectMapper.readValue(it) }
