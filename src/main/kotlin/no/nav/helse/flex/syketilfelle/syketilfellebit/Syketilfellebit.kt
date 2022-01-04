package no.nav.helse.flex.syketilfelle.syketilfellebit

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Table("syketilfellebit")
data class SyketilfellebitDbRecord(
    @Id
    val id: String? = null,
    val syketilfellebitId: String,
    val fnr: String,
    val orgnummer: String?,
    val opprettet: OffsetDateTime,
    val inntruffet: OffsetDateTime,
    val tags: String,
    val ressursId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val korrigererSendtSoknad: String?,
    val publisert: Boolean,
)

fun SyketilfellebitDbRecord.tilSyketilfellebit(): Syketilfellebit = Syketilfellebit(
    id = null,
    syketilfellebitId = syketilfellebitId,
    fnr = fnr,
    orgnummer = orgnummer,
    opprettet = opprettet,
    inntruffet = inntruffet,
    tags = tags.tagsFromString(),
    ressursId = ressursId,
    fom = fom,
    tom = tom,
    korrigererSendtSoknad = korrigererSendtSoknad,
    publisert = publisert,
)

fun SyketilfellebitDbRecord.tilKafkasyketilfellebit(): KafkaSyketilfellebit = KafkaSyketilfellebit(
    id = syketilfellebitId,
    fnr = fnr,
    orgnummer = orgnummer,
    opprettet = opprettet,
    inntruffet = inntruffet,
    tags = tags.tagsFromString().map { it.name }.toSet(),
    ressursId = ressursId,
    fom = fom,
    tom = tom,
    korrigererSendtSoknad = korrigererSendtSoknad,
)

fun Syketilfellebit.tilSyketilfellebitDbRecord(): SyketilfellebitDbRecord = SyketilfellebitDbRecord(
    id = null,
    syketilfellebitId = syketilfellebitId,
    fnr = fnr,
    orgnummer = orgnummer,
    opprettet = opprettet,
    inntruffet = inntruffet,
    tags = tags.somString(),
    ressursId = ressursId,
    fom = fom,
    tom = tom,
    korrigererSendtSoknad = korrigererSendtSoknad,
    publisert = publisert,
)

fun Set<String>.asString() = this.joinToString(",")
fun Set<Tag>.somString() = this.map { it.name }.toSet().asString()

fun String.tagsFromString() = split(',').map(String::trim).map(Tag::valueOf).toSet()

data class Syketilfellebit(
    val id: String? = null,
    val syketilfellebitId: String = UUID.randomUUID().toString(),
    val fnr: String,
    val orgnummer: String?,
    val opprettet: OffsetDateTime,
    val inntruffet: OffsetDateTime,
    val tags: Set<Tag>,
    val ressursId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val korrigererSendtSoknad: String? = null,
    val publisert: Boolean = false,
)

enum class Tag {
    SYKMELDING,
    NY,
    BEKREFTET,
    SENDT,
    KORRIGERT,
    AVBRUTT,
    UTGAATT,
    PERIODE,
    FULL_AKTIVITET,
    INGEN_AKTIVITET,
    GRADERT_AKTIVITET,
    BEHANDLINGSDAGER,
    BEHANDLINGSDAG,
    ANNET_FRAVAR,
    SYKEPENGESOKNAD,
    FERIE,
    PERMISJON,
    OPPHOLD_UTENFOR_NORGE,
    EGENMELDING,
    FRAVAR_FOR_SYKMELDING,
    PAPIRSYKMELDING,
    ARBEID_GJENNOPPTATT,
    KORRIGERT_ARBEIDSTID,
    UKJENT_AKTIVITET,
    UTDANNING,
    FULLTID,
    DELTID,
    REDUSERT_ARBEIDSGIVERPERIODE,
    REISETILSKUDD,
    AVVENTENDE
}
