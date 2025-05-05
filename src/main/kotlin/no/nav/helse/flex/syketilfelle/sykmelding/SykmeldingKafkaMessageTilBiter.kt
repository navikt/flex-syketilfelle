package no.nav.helse.flex.syketilfelle.sykmelding

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.helse.flex.syketilfelle.extensions.tilOsloZone
import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.syfo.model.sykmelding.arbeidsgiver.SykmeldingsperiodeAGDTO
import no.nav.syfo.model.sykmelding.model.*
import no.nav.syfo.model.sykmeldingstatus.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.ArrayList

private val objectMapper =
    ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)

fun SykmeldingKafkaMessage.mapTilEgenmeldingBiter(): List<Syketilfellebit> =
    event.sporsmals
        ?.firstOrNull { it.shortName == ShortNameDTO.EGENMELDINGSDAGER }
        ?.svar
        ?.let { objectMapper.readValue(it) as List<String> }
        ?.map { LocalDate.parse(it) }
        ?.groupConsecutiveDays()
        ?.map {
            Syketilfellebit(
                orgnummer = this.event.arbeidsgiver?.orgnummer,
                inntruffet = this.event.timestamp.tilOsloZone(),
                fom = it.fom,
                tom = it.tom,
                opprettet = OffsetDateTime.now().tilOsloZone(),
                ressursId = this.sykmelding.id,
                tags = setOf(Tag.SYKMELDING, Tag.SENDT, Tag.EGENMELDING),
                fnr = this.kafkaMetadata.fnr,
            )
        }
        ?: emptyList()

fun SykmeldingKafkaMessage.mapTilBiter(): List<Syketilfellebit> {
    val sykmeldingsperioderBiter =
        this.sykmelding
            .sykmeldingsperioder
            .map {
                Syketilfellebit(
                    orgnummer = this.event.arbeidsgiver?.orgnummer,
                    inntruffet =
                        if (this.event.statusEvent == STATUS_APEN) {
                            OffsetDateTime.now()
                        } else {
                            this.event.timestamp.tilOsloZone()
                        },
                    fom = it.fom,
                    tom = it.tom,
                    opprettet = OffsetDateTime.now().tilOsloZone(),
                    ressursId = this.sykmelding.id,
                    tags = it.finnTagsForPeriode(this),
                    fnr = this.kafkaMetadata.fnr,
                )
            }

    val periodeSvar =
        this.event.sporsmals
            ?.firstOrNull { it.shortName == ShortNameDTO.PERIODE }
            ?.svar
            ?.let {
                return@let objectMapper.readValue(it) as List<Periode>
            }?.map {
                Syketilfellebit(
                    orgnummer = this.event.arbeidsgiver?.orgnummer,
                    inntruffet = this.event.timestamp.tilOsloZone(),
                    fom = it.fom,
                    tom = it.tom,
                    opprettet = OffsetDateTime.now().tilOsloZone(),
                    ressursId = this.sykmelding.id,
                    tags = setOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.ANNET_FRAVAR),
                    fnr = this.kafkaMetadata.fnr,
                )
            }
            ?: emptyList()

    return ArrayList<Syketilfellebit>().also {
        it.addAll(sykmeldingsperioderBiter)
        it.addAll(periodeSvar)
        it.addAll(mapTilEgenmeldingBiter())
    }
}

fun List<LocalDate>.groupConsecutiveDays(): List<Periode> =
    this
        .sorted()
        .fold(emptyList()) { perioder, dato ->
            if (perioder.isEmpty() || perioder.last().tom.plusDays(1) != dato) {
                perioder + Periode(dato, dato)
            } else {
                perioder.dropLast(1) + Periode(fom = perioder.last().fom, tom = dato)
            }
        }

private fun SykmeldingsperiodeAGDTO.finnTagsForPeriode(sykmeldingKafkaMessage: SykmeldingKafkaMessage): Set<Tag> {
    val tags = ArrayList<Tag>()

    tags.add(Tag.SYKMELDING)
    tags.add(sykmeldingKafkaMessage.finnStatustag())
    tags.add(Tag.PERIODE)

    fun SykmeldingsperiodeAGDTO.finnTagsForGrad(): Tag {
        val grad = this.gradert?.grad

        return when (this.type) {
            PeriodetypeDTO.BEHANDLINGSDAGER -> Tag.BEHANDLINGSDAGER
            PeriodetypeDTO.AKTIVITET_IKKE_MULIG -> Tag.INGEN_AKTIVITET
            else -> {
                when {
                    grad == null -> Tag.UKJENT_AKTIVITET
                    grad <= 0 -> Tag.FULL_AKTIVITET
                    grad >= 100 -> Tag.INGEN_AKTIVITET
                    else -> Tag.GRADERT_AKTIVITET
                }
            }
        }
    }

    if (this.reisetilskudd || this.type == PeriodetypeDTO.REISETILSKUDD) {
        tags.add(Tag.REISETILSKUDD)
    }
    if (this.type == PeriodetypeDTO.AVVENTENDE) {
        tags.add(Tag.AVVENTENDE)
    }
    tags.add(this.finnTagsForGrad())

    if (sykmeldingKafkaMessage.sykmelding.harRedusertArbeidsgiverperiode) {
        tags.add(Tag.REDUSERT_ARBEIDSGIVERPERIODE)
    }

    return tags.toSet()
}

private fun SykmeldingKafkaMessage.finnStatustag(): Tag =
    when (val statusEvent = this.event.statusEvent) {
        STATUS_APEN -> Tag.NY
        STATUS_AVBRUTT -> Tag.AVBRUTT
        STATUS_UTGATT -> Tag.UTGAATT
        STATUS_SENDT -> Tag.SENDT
        STATUS_BEKREFTET -> Tag.BEKREFTET
        else -> {
            throw IllegalStateException("Mottok en ukjent statusevent: $statusEvent")
        }
    }
