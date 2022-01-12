package no.nav.helse.flex.syketilfelle.identer

import no.nav.security.token.support.core.context.TokenValidationContextHolder

fun TokenValidationContextHolder.fnrFraLoginservicetoken(): String {
    val context = this.tokenValidationContext
    return context.getClaims("loginservice").subject
}
