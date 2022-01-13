package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain

import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import java.time.LocalDate

class Syketilfelledag(
    val dag: LocalDate,
    val prioritertSyketilfellebit: Syketilfellebit?,
    val syketilfellebiter: List<Syketilfellebit>
)
