package no.difi.meldingsutveksling.exceptions

import org.springframework.http.HttpStatus

class InvalidCertificateException(s: String) : HttpStatusCodeException(HttpStatus.BAD_REQUEST, InvalidCertificateException::class.qualifiedName, s) {
}