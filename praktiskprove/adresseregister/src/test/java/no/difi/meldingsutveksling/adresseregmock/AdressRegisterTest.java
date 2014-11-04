package no.difi.meldingsutveksling.adresseregmock;

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

}
