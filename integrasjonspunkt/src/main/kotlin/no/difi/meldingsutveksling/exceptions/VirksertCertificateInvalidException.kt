package no.difi.meldingsutveksling.exceptions

import org.springframework.http.HttpStatus

class VirksertCertificateInvalidException(s: String) : HttpStatusCodeException(HttpStatus.BAD_REQUEST, VirksertCertificateInvalidException::class.qualifiedName, s) {
}