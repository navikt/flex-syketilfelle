package no.nav.helse.flex.syketilfelle.identer

import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.exceptionhandler.ApiErrorException
import no.nav.helse.flex.syketilfelle.exceptionhandler.LogLevel
import org.springframework.http.HttpStatus

interface MedPdlClient {
    val pdlClient: PdlClient

    fun List<String>.validerFnrOgHentAndreIdenter(hentAndreIdenter: Boolean): List<String> {
        if (this.isEmpty()) {
            throw ManglerIdenterException()
        }

        fun String.isDigit(): Boolean = this.all { it.isDigit() }

        if (this.any { !it.isDigit() || it.length != 11 }) {
            throw FeilIdentException()
        }

        return if (hentAndreIdenter) {
            if (this.size != 1) {
                throw FlereIdenterVedHentingException()
            }
            pdlClient.hentFolkeregisterIdenter(this.first())
        } else {
            this
        }
    }
}

class FlereIdenterVedHentingException :
    ApiErrorException(
        message = "Kan ikke ha flere identer i input når vi skal hente flere identer",
        httpStatus = HttpStatus.BAD_REQUEST,
        reason = "FLERE_IDENTER_OG_HENTING",
        loglevel = LogLevel.ERROR,
    )

class ManglerIdenterException :
    ApiErrorException(
        message = "Må ha hvertfall en ident i input",
        httpStatus = HttpStatus.BAD_REQUEST,
        reason = "MANGLER_IDENTER",
        loglevel = LogLevel.ERROR,
    )

class FeilIdentException :
    ApiErrorException(
        message = "Forventer ident med 11 siffer",
        httpStatus = HttpStatus.BAD_REQUEST,
        reason = "UGYLDIG_FNR",
        loglevel = LogLevel.ERROR,
    )
