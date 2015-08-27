package no.difi.meldingsutveksling.adresseregister;

import java.security.PublicKey;
import java.security.cert.Certificate;

/**
 * @author Glenn Bech
 */

public interface AddressRegister {

    /**
     * Convenience method. Extracts hte public key from certificate
     *
     * @param orgNumber
     * @return
     */
    @Deprecated
    PublicKey getPublicKey(String orgNumber);

    /**
     * This method gets the certificate for an organisation.
     *
     * @param orgNumber
     * @return
     */
    @Deprecated
    Certificate getCertificate(String orgNumber);


    String getCeritifcateString(String orgNumber);


}
