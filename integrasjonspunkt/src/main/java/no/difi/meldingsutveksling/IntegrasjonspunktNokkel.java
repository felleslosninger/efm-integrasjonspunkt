package no.difi.meldingsutveksling;

import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
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


    private final String pkLocation, pkAlias, pkPassword;

    public IntegrasjonspunktNokkel() {

        IntegrasjonspunktConfig config = IntegrasjonspunktConfig.getInstance();

        pkAlias = config.getPrivateKeyAlias();
        pkLocation = config.getKeyStoreLocation();
        pkPassword = config.getPrivateKeyPassword();

        if (pkAlias == null) {
            throw new MeldingsUtvekslingRuntimeException("Missing private key alias system property");
        }
        if (pkLocation == null) {
            throw new MeldingsUtvekslingRuntimeException("Missing private key location system property");
        }
        if (pkPassword == null) {
            throw new MeldingsUtvekslingRuntimeException("Missing private key password system property");
        }
    }

    public IntegrasjonspunktNokkel(String pkLocation, String pkAlias, String pkPassword) {
        this.pkLocation = pkLocation;
        this.pkAlias = pkAlias;
        this.pkPassword = pkPassword;
    }

    /**
     * Loads the private key from the keystore
     *
     * @return the private key
     */
    public PrivateKey loadPrivateKey() {

        PrivateKey key = null;
        try (InputStream i = openKeyInputStream()) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(i, pkPassword.toCharArray());

            Enumeration aliases = keystore.aliases();
            for (; aliases.hasMoreElements(); ) {
                String alias = (String) aliases.nextElement();
                boolean isKey = keystore.isKeyEntry(alias);
                if (isKey && alias.equals(pkAlias)) {
                    key = (PrivateKey) keystore.getKey(alias, pkPassword.toCharArray());
                }
            }
            if (key == null) {
                throw new MeldingsUtvekslingRuntimeException("no key with alias " + pkAlias + " found in the keystore " + pkLocation);
            }
            return key;
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public SignatureHelper getSignatureHelper() {
        try {
            InputStream keyInputStream = openKeyInputStream();
            return new SignatureHelper(keyInputStream, pkPassword, pkAlias, pkPassword);
        } catch (FileNotFoundException e) {
            throw new MeldingsUtvekslingRuntimeException("keystore " + pkLocation + " not found on file system.");
        }
    }

    private InputStream openKeyInputStream() throws FileNotFoundException {
        return new FileInputStream(pkLocation);
    }

}

