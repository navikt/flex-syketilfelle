package no.nav.helse.flex.syketilfelle.syketilfellebit

import org.springframework.data.annotation.Id
import java.time.LocalDate
import java.time.OffsetDateTime

data class Syketilfellebit(
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
)
