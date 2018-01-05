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

    protected final KeyStoreProperties properties;

    protected KeyStore keyStore;

    public IntegrasjonspunktNokkel(KeyStoreProperties properties) {

        this.properties = properties;

        try {
            this.keyStore = KeystoreProvider.loadKeyStore(properties);
        }catch (KeystoreProviderException e){
            throw new IllegalStateException(e);
        }
    }

    /**
     * Loads the private key from the keystore
     *
     * @return the private key
     */
    public PrivateKey loadPrivateKey() {

        char[] password = properties.getPassword().toCharArray();

        try {
            return (PrivateKey) keyStore.getKey(properties.getAlias(), password);

        }catch (KeyStoreException | NoSuchAlgorithmException e) {

            throw new IllegalStateException("Unexpected problem when operating KeyStore", e);
        }catch (UnrecoverableEntryException e){

            throw new IllegalStateException(
                    String.format("No PrivateKey with alias \"%s\" found in the KeyStore or the provided Password is wrong", properties.getAlias())
                    ,e
            );

        }
    }

    public X509Certificate getX509Certificate() {

        try {
            if( !keyStore.containsAlias(properties.getAlias()) ){
                throw new IllegalStateException(
                        String.format("No Certificate with alias \"%s\" found in the KeyStore", properties.getAlias())
                );
            }

            return (X509Certificate) keyStore.getCertificate(properties.getAlias());
        }catch (KeyStoreException e){

            throw new IllegalStateException("Unexpected problem when operating KeyStore", e);
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


    public KeyStore getKeyStore() {
        return keyStore;
    }

    public class MoveSignaturHelper extends SignatureHelper {

        public MoveSignaturHelper(KeyStore keyStore, String keyAlias, String keyPassword)  {

            super(null);
            loadCertificate(keyStore, keyAlias, keyPassword);
        }
    }
}
