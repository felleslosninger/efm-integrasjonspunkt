package no.difi.meldingsutveksling.validation

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@Profile("!test")
class KeystoreExpirationValidator(
    val integrasjonspunktNokkel: IntegrasjonspunktNokkel
) {

    @PostConstruct
    fun validateKeystore() {
        integrasjonspunktNokkel.x509Certificate.checkValidity()
    }

}