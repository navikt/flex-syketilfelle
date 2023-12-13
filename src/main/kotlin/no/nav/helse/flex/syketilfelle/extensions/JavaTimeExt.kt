package no.nav.helse.flex.syketilfelle.extensions

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

val osloZone = ZoneId.of("Europe/Oslo")

fun LocalDateTime.tilOsloZone(): OffsetDateTime = this.atZone(osloZone).toOffsetDateTime()

fun OffsetDateTime.tilOsloZone(): OffsetDateTime = this.atZoneSameInstant(osloZone).toOffsetDateTime()
