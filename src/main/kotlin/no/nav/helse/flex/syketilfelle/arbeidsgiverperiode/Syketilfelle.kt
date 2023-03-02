package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.ListContainsPredicate.Companion.tagsSize
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Oppfolgingstilfelle
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Syketilfellebiter
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Syketilfelledag
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Tidslinje
import no.nav.helse.flex.syketilfelle.extensions.osloZone
import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

fun genererOppfolgingstilfelle(
    fnrs: List<String>,
    biter: List<Syketilfellebit>,
    andreKorrigerteRessurser: List<String> = emptyList(),
    tilleggsbiter: List<Syketilfellebit> = emptyList(),
    grense: LocalDateTime? = null,
    startSyketilfelle: LocalDate? = null
): List<Oppfolgingstilfelle>? {
    var syketilfellebiter = biter

    startSyketilfelle?.let {
        val registertSykemeldingFomForSyketille = syketilfellebiter
            .asSequence()
            .filter { it.tags.containsAny(SYKMELDING, PAPIRSYKMELDING) }
            .map { it.fom }
            .filter { it.isEqualOrAfter(startSyketilfelle) }
            .sorted()
            .firstOrNull()
        registertSykemeldingFomForSyketille?.let {
            if (registertSykemeldingFomForSyketille.isAfter(startSyketilfelle)) {
                val teoretiskPapirsykemelding = Syketilfellebit(
                    fnr = fnrs.first(),
                    fom = startSyketilfelle,
                    inntruffet = startSyketilfelle.atStartOfDay().atZone(osloZone).toOffsetDateTime(),
                    opprettet = startSyketilfelle.atStartOfDay().atZone(osloZone).toOffsetDateTime(),
                    tom = registertSykemeldingFomForSyketille.minusDays(1),
                    ressursId = UUID.randomUUID().toString(),
                    tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                    orgnummer = null
                )
                syketilfellebiter = syketilfellebiter.toMutableList().also { it.add(teoretiskPapirsykemelding) }
            }
        }
    }
    return genererOppfolgingstilfelle(
        syketilfellebiter,
        andreKorrigerteRessurser,
        tilleggsbiter,
        grense
    )
}

fun genererOppfolgingstilfelle(
    biter: List<Syketilfellebit>,
    andreKorrigerteRessurser: List<String> = emptyList(),
    tilleggsbiter: List<Syketilfellebit> = emptyList(),
    grense: LocalDateTime? = null
): List<Oppfolgingstilfelle>? {
    val korrigerteBiter = biter.finnBiterSomTilleggsbiterVilKorrigere(andreKorrigerteRessurser, tilleggsbiter)

    val merge = ArrayList(biter)
        .apply {
            addAll(tilleggsbiter)
            addAll(korrigerteBiter)
        }
        .filtrerBortKorrigerteBiter()

    if (merge.isEmpty()) {
        return null
    }

    val tidslinje = Tidslinje(
        Syketilfellebiter(
            prioriteringsliste = listOf(
                SYKEPENGESOKNAD and SENDT and ARBEID_GJENNOPPTATT,
                SYKEPENGESOKNAD and SENDT and KORRIGERT_ARBEIDSTID and BEHANDLINGSDAGER,
                SYKEPENGESOKNAD and SENDT and KORRIGERT_ARBEIDSTID and FULL_AKTIVITET,
                SYKEPENGESOKNAD and SENDT and KORRIGERT_ARBEIDSTID and (GRADERT_AKTIVITET or INGEN_AKTIVITET),
                SYKEPENGESOKNAD and SENDT and (PERMISJON or FERIE),
                SYKEPENGESOKNAD and SENDT and (EGENMELDING or PAPIRSYKMELDING or FRAVAR_FOR_SYKMELDING),
                SYKEPENGESOKNAD and SENDT and tagsSize(2),
                SYKEPENGESOKNAD and SENDT and BEHANDLINGSDAG,
                SYKEPENGESOKNAD and SENDT and BEHANDLINGSDAGER,
                SYKMELDING and (SENDT or BEKREFTET) and PERIODE and BEHANDLINGSDAGER,
                SYKMELDING and (SENDT or BEKREFTET) and PERIODE and FULL_AKTIVITET,
                SYKMELDING and (SENDT or BEKREFTET) and PERIODE and (GRADERT_AKTIVITET or INGEN_AKTIVITET),
                SYKMELDING and BEKREFTET and ANNET_FRAVAR,
                SYKMELDING and SENDT and PERIODE and REISETILSKUDD and UKJENT_AKTIVITET,
                SYKMELDING and NY and PERIODE and BEHANDLINGSDAGER,
                SYKMELDING and NY and PERIODE and FULL_AKTIVITET,
                SYKMELDING and NY and PERIODE and (GRADERT_AKTIVITET or INGEN_AKTIVITET),
                SYKMELDING and NY and PERIODE and REISETILSKUDD and UKJENT_AKTIVITET

            ),
            biter = merge
        )
    )

    return grupperIOppfolgingstilfeller(
        tidslinje
            .tidslinjeSomListe()
            .filterBortBiterEtter(grense)
    )
}

private fun List<Syketilfelledag>.filterBortBiterEtter(grense: LocalDateTime?) =
    if (grense != null) filter { !it.dag.isAfter(grense.toLocalDate()) } else this

fun Set<Tag>.containsAny(vararg tags: Tag): Boolean {
    for (tag in tags) {
        if (this.contains(tag)) {
            return true
        }
    }
    return false
}

fun Set<Tag>.erstattTag(fra: Tag, til: Tag): Set<Tag> =
    ArrayList(this).apply {
        add(this.indexOf(fra), til)
        remove(fra)
    }.toSet()

private fun List<Syketilfellebit>.filtrerBortKorrigerteBiter(): List<Syketilfellebit> =
    filterNot { muligSendtBit ->
        muligSendtBit.tags.toList() in (SYKEPENGESOKNAD and SENDT) && any { muligKorrigertBit ->
            muligKorrigertBit.tags.toList() in (SYKEPENGESOKNAD and KORRIGERT) &&
                muligKorrigertBit.ressursId == muligSendtBit.ressursId
        }
    }

private fun List<Syketilfellebit>.finnBiterSomTilleggsbiterVilKorrigere(
    andreKorrigerteRessurser: List<String>,
    tilleggsbiter: List<Syketilfellebit>
): List<Syketilfellebit> =
    filter { it.tags.toList() in (SYKEPENGESOKNAD and SENDT) }
        .filter { it.ressursId in andreKorrigerteRessurser }
        .map { bitSomSkalKorrigeres ->
            val forsteTilleggsbitInntruffet = tilleggsbiter
                .filter { it.tags.toList() in (SYKEPENGESOKNAD and SENDT) }
                .sortedBy { it.inntruffet }
                .firstOrNull()
                ?.inntruffet
                ?: OffsetDateTime.now()
            Syketilfellebit(
                id = bitSomSkalKorrigeres.id,
                orgnummer = bitSomSkalKorrigeres.orgnummer,
                opprettet = OffsetDateTime.now(),
                inntruffet = forsteTilleggsbitInntruffet,
                tags = bitSomSkalKorrigeres.tags.erstattTag(SENDT, KORRIGERT),
                ressursId = bitSomSkalKorrigeres.ressursId,
                fom = bitSomSkalKorrigeres.fom,
                tom = bitSomSkalKorrigeres.tom,
                fnr = bitSomSkalKorrigeres.fnr
            )
        }
