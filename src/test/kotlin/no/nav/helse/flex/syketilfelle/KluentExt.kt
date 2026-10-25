package no.nav.helse.flex.syketilfelle

import io.kotest.matchers.shouldBe
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Suppress("ktlint:standard:function-naming")
infix fun OffsetDateTime.`should be equal to ignoring nano and zone`(expected: OffsetDateTime) =
    this.trunacteNanoUTC() shouldBe expected.trunacteNanoUTC()

fun OffsetDateTime.trunacteNanoUTC(): OffsetDateTime = this.truncatedTo(ChronoUnit.MILLIS).toInstant().atOffset(ZoneOffset.UTC)
