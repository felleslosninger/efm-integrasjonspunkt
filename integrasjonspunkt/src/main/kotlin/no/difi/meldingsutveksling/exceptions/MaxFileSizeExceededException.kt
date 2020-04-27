package no.difi.meldingsutveksling.exceptions

import org.springframework.http.HttpStatus

class MaxFileSizeExceededException(size: String, service: String, limit: String): HttpStatusCodeException(HttpStatus.BAD_REQUEST, MaxFileSizeExceededException::class.qualifiedName, size, service, limit)