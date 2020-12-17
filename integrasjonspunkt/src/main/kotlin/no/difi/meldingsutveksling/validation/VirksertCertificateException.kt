package no.difi.meldingsutveksling.validation

import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException

class VirksertCertificateException : RuntimeException {
    constructor(t: ServiceRegistryLookupException) : super(t)
    constructor(s: String) : super(s)
}