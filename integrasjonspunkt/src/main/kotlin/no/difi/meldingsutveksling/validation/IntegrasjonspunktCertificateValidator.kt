package no.difi.meldingsutveksling.validation

import no.difi.meldingsutveksling.CertificateParser
import no.difi.meldingsutveksling.CertificateParserException
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryClient
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.security.cert.CertificateExpiredException
import javax.annotation.PostConstruct

@Component
@Profile("{!(test | cucumber)}")
class IntegrasjonspunktCertificateValidator(
    private val ipNokkel: IntegrasjonspunktNokkel,
    private val props: IntegrasjonspunktProperties,
    private val srClient: ServiceRegistryClient
) {

    @PostConstruct
    @Throws(VirksertCertificateException::class, CertificateExpiredException::class)
    fun validateCertificate() {
        ipNokkel.x509Certificate.checkValidity()

        val pem = try {
            srClient.getCertificate(props.org.number)
        } catch (e: ServiceRegistryLookupException) {
            throw VirksertCertificateException(e)
        }

        val cert = try {
            CertificateParser.parse(pem)
        } catch (e: CertificateParserException) {
            throw VirksertCertificateException("Failed to parse certificate from Virksert", e)
        }
        cert.checkValidity()

        if (ipNokkel.x509Certificate.serialNumber != cert.serialNumber) {
            throw VirksertCertificateException("Keystore certificate serial number (${ipNokkel.x509Certificate.serialNumber}) does not match certificate in Virksert (${cert.serialNumber})")
        }
    }

}