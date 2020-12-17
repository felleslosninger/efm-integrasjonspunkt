package no.difi.meldingsutveksling.validation

import no.difi.meldingsutveksling.CertificateParser
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryClient
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@Profile("!test")
class CertificateExpirationValidator(
    val integrasjonspunktNokkel: IntegrasjonspunktNokkel,
    val props: IntegrasjonspunktProperties,
    val srClient: ServiceRegistryClient
) {

    @PostConstruct
    fun validateCertificate() {
        integrasjonspunktNokkel.x509Certificate.checkValidity()

        try {
            val certificate = srClient.getCertificate(props.org.number)
            CertificateParser().parse(certificate).checkValidity()
        } catch (e: ServiceRegistryLookupException) {
            throw CertificateNotInVirksertException(e)
        }
    }

}