package no.difi.meldingsutveksling.ks.svarinn

internal data class ErrorResponse(val feilmelding: String, val permanent: Boolean = true)