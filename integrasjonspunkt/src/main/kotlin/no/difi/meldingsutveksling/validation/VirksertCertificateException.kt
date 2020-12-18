package no.difi.meldingsutveksling.validation

class VirksertCertificateException : RuntimeException {
    constructor(t: Throwable) : super(t)
    constructor(s: String) : super(s)
    constructor(s: String, t: Throwable) : super(s, t)
}