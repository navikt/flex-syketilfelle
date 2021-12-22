package no.nav.helse.flex.syketilfelle.syketilfellebit

import java.time.LocalDate
import java.time.OffsetDateTime

data class Syketilfellebit(
    val id: String,
    val syketilfellebitId: String,
    val fnr: String,
    val orgnummer: String?,
    val opprettet: OffsetDateTime,
    val inntruffet: OffsetDateTime,
    val tags: List<String>,
    val ressursId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val korrigererSendtSoknad: String?,
)
