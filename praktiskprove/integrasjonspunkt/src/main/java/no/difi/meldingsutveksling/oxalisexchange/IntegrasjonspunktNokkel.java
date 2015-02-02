package no.difi.meldingsutveksling.oxalisexchange;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Enumeration;

/**

 */
public class IntegrasjonspunktNokkel {

    public static final String PRIVATEKEYALIAS = "privatekeyalias";
    public static final String PRIVATEKEYLOACATION = "privatekeyloacation";
    public static final String PRIVATEKEYPASSWORD = "privatekeypassword";

    private String pkResource, pkAlias, pkPasswprd;


    public IntegrasjonspunktNokkel() {

        pkAlias = System.getProperty(PRIVATEKEYALIAS);
        pkResource = System.getProperty(PRIVATEKEYLOACATION);
        pkPasswprd = System.getProperty(PRIVATEKEYPASSWORD);

        if (pkAlias == null) {
            throw new MeldingsUtvekslingRuntimeException("please start the integrajonspunkt with a system property called " + PRIVATEKEYALIAS + ", that names the alias e of the private key within the keystore.");
        }
        if (pkResource == null) {
            throw new MeldingsUtvekslingRuntimeException("please start the integrajonspunkt with a system property called " + PRIVATEKEYLOACATION + ", that points to a class path resource of the private key file");
        }
        if (pkPasswprd == null) {
            throw new MeldingsUtvekslingRuntimeException("please start the integrajonspunkt with a system property called " + PRIVATEKEYPASSWORD);
        }

    }

    public IntegrasjonspunktNokkel(String pkResource, String pkAlias, String pkPasswprd) {
        this.pkResource = pkResource;
        this.pkAlias = pkAlias;
        this.pkPasswprd = pkPasswprd;
    }

    /**
     * Loads the private key from a pkcs8 file
     *
     * @return an private key
     * @throws java.io.IOException
     */
    public PrivateKey loadPrivateKey() {

        PrivateKey key = null;
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(pkResource);
            keystore.load(resourceAsStream, pkPasswprd.toCharArray());

            Enumeration aliases = keystore.aliases();
            for (; aliases.hasMoreElements(); ) {
                String alias = (String) aliases.nextElement();
                boolean isKey = keystore.isKeyEntry(alias);
                if (isKey && alias.equals(pkAlias)) {
                    key = (PrivateKey) keystore.getKey(alias, pkPasswprd.toCharArray());
                }
            }
            if (key == null) {
                throw new MeldingsUtvekslingRuntimeException("no key with alias " + pkAlias + " found in the class path location " + pkResource);
            }
            return key;
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}
