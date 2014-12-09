package no.difi.meldingsutveksling.adresseregister;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

/**
 *
 *
 *
 * @author Glenn Bech
 */
public class AdressRegisterTest {

    @Test
    public void shouldGetPublicKeyOfOrganisation() {
        assertNotNull(AdressRegisterFactory.createAdressRegister().getPublicKey("958935429"));
    }

    @Test
    public void shouldGetCertificateForOrganisation() {
        assertNotNull(AdressRegisterFactory.createAdressRegister().getCertificate("960885406"));
        assertNotNull(AdressRegisterFactory.createAdressRegister().getCertificate("958935429"));
    }

}
