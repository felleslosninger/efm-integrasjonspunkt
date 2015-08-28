package no.difi.meldingsutveksling.oxalisexchange;

import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Enumeration;

/**
 * Class responsible for accessing the keystore for the Integrasjonspunkt.
 *
 * @author Glebnn Bech
 */
public class IntegrasjonspunktNokkel {

    private static final String PRIVATEKEYALIAS = "privatekeyalias";
    private static final String PRIVATEKEYLOACATION = "keystorelocation";
    private static final String PRIVATEKEYPASSWORD = "privatekeypassword";

    private final String pkResource, pkAlias, pkPasswprd;

    public IntegrasjonspunktNokkel() {

        pkAlias = System.getProperty(PRIVATEKEYALIAS);
        pkResource = System.getProperty(PRIVATEKEYLOACATION);
        pkPasswprd = System.getProperty(PRIVATEKEYPASSWORD);

        if (pkAlias == null) {
            throw new MeldingsUtvekslingRuntimeException("please start the process with a system property called " + PRIVATEKEYALIAS + ", that names the alias e of the private key within the keystore.");
        }
        if (pkResource == null) {
            throw new MeldingsUtvekslingRuntimeException("please start the process with a system property called " + PRIVATEKEYLOACATION + ", that points to a file the keytstore");
        }
        if (pkPasswprd == null) {
            throw new MeldingsUtvekslingRuntimeException("please start the process with a system property called " + PRIVATEKEYPASSWORD);
        }

    }

    public IntegrasjonspunktNokkel(String pkResource, String pkAlias, String pkPasswprd) {
        this.pkResource = pkResource;
        this.pkAlias = pkAlias;
        this.pkPasswprd = pkPasswprd;
    }

    /**
     * Loads the private key from the keystore
     *
     * @return the private key
     */
    public PrivateKey loadPrivateKey() {

        PrivateKey key = null;
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            InputStream resourceAsStream = getKeyInputStream();
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
                throw new MeldingsUtvekslingRuntimeException("no key with alias " + pkAlias + " found in the keystore " + pkResource);
            }
            return key;
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public SignatureHelper getSignatureHelper() {
        try {
            InputStream keyInputStream = getKeyInputStream();
            return new SignatureHelper(keyInputStream, pkPasswprd, pkAlias, pkPasswprd);
        } catch (FileNotFoundException e) {
            throw new MeldingsUtvekslingRuntimeException("keystore " + pkResource + " not found on classpath.");
        }
    }

    private InputStream getKeyInputStream() throws FileNotFoundException {
        return new FileInputStream(pkResource);
    }

}

