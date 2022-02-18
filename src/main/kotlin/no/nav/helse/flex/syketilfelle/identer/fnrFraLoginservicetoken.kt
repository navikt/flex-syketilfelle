package no.nav.helse.flex.syketilfelle.identer

import no.nav.security.token.support.core.context.TokenValidationContextHolder

fun TokenValidationContextHolder.fnrFraLoginservicetoken(): String {
    val claims = this.tokenValidationContext.getClaims("loginservice")
    return claims.getStringClaim("pid") ?: claims.subject
}
