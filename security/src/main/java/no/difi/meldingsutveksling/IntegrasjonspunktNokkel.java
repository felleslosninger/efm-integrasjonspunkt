package no.difi.meldingsutveksling;

import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.config.KeyStoreProperties;
import no.difi.meldingsutveksling.lang.KeystoreProviderException;

import java.security.*;
import java.security.cert.X509Certificate;

/**
 * Class responsible for accessing the keystore for the Integrasjonspunkt.
 *
 * @author Glebnn Bech
 */
public class IntegrasjonspunktNokkel {

    private static final String ERR_MISSING_PRIVATE_KEY_OR_PASS = "Problem accessing PrivateKey with alias \"%s\" inadequate access or Password is wrong";
    private static final String ERR_MISSING_PRIVATE_KEY = "No PrivateKey with alias \"%s\" found in the KeyStore";
    private static final String ERR_MISSING_CERTIFICATE = "No Certificate with alias \"%s\" found in the KeyStore";
    private static final String ERR_GENERAL = "Unexpected problem occurred when operating KeyStore";

    private final KeyStoreProperties properties;
    private final KeyStore keyStore;

    public IntegrasjonspunktNokkel(KeyStoreProperties properties) {

        this.properties = properties;

        try {
            this.keyStore = KeystoreProvider.loadKeyStore(properties);
        } catch (KeystoreProviderException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Loads the private key from the keystore
     *
     * @return the private key
     */
    public PrivateKey loadPrivateKey() {

        PrivateKey privateKey;

        char[] password = properties.getPassword().toCharArray();

        try {

            privateKey = (PrivateKey) keyStore.getKey(properties.getAlias(), password);

            if (privateKey == null) {

                throw new IllegalStateException(
                        String.format(ERR_MISSING_PRIVATE_KEY, properties.getAlias())
                );
            }

        } catch (KeyStoreException | NoSuchAlgorithmException e) {

            throw new IllegalStateException(ERR_GENERAL, e);
        } catch (UnrecoverableEntryException e) {

            throw new IllegalStateException(
                    String.format(ERR_MISSING_PRIVATE_KEY_OR_PASS, properties.getAlias())
                    , e
            );

        }

        return privateKey;
    }

    public X509Certificate getX509Certificate() {

        try {

            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(properties.getAlias());

            if (certificate == null) {
                throw new IllegalStateException(
                        String.format(ERR_MISSING_CERTIFICATE, properties.getAlias())
                );
            }

            return certificate;
        } catch (KeyStoreException e) {
            throw new IllegalStateException(ERR_GENERAL, e);
        }
    }

    public KeyPair getKeyPair() {
        PrivateKey privateKey = loadPrivateKey();
        X509Certificate certificate = getX509Certificate();

        return new KeyPair(certificate.getPublicKey(), privateKey);
    }

    public SignatureHelper getSignatureHelper() {
        return new MoveSignaturHelper(keyStore, properties.getAlias(), properties.getPassword());
    }

    public boolean shouldLockProvider() {
        return properties.getLockProvider();
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public class MoveSignaturHelper extends SignatureHelper {

        MoveSignaturHelper(KeyStore keyStore, String keyAlias, String keyPassword) {

            super(properties.getLockProvider() ? keyStore.getProvider() : null);
            loadCertificate(keyStore, keyAlias, keyPassword);
        }
    }
}
