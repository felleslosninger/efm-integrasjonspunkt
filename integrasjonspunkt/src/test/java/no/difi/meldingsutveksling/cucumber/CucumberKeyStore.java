package no.difi.meldingsutveksling.cucumber;

import no.difi.meldingsutveksling.KeystoreProvider;
import no.difi.meldingsutveksling.config.KeyStoreProperties;
import no.difi.meldingsutveksling.lang.KeystoreProviderException;

import java.security.*;

public class CucumberKeyStore {
    private static final String ERR_MISSING_PRIVATE_KEY_OR_PASS = "Problem accessing PrivateKey with alias \"%s\" inadequate access or Password is wrong";
    private static final String ERR_MISSING_PRIVATE_KEY = "No PrivateKey with alias \"%s\" found in the KeyStore";
    private static final String ERR_GENERAL = "Unexpected problem occurred when operating KeyStore";

    protected final KeyStoreProperties properties;

    private final KeyStore keyStore;

    public CucumberKeyStore(KeyStoreProperties properties) {

        this.properties = properties;

        try {
            this.keyStore = KeystoreProvider.loadKeyStore(properties);
        } catch (KeystoreProviderException e) {
            throw new IllegalStateException(e);
        }
    }

    public PrivateKey getPrivateKey(String alias) {

        PrivateKey privateKey;

        char[] password = properties.getPassword().toCharArray();

        try {

            privateKey = (PrivateKey) keyStore.getKey(alias, password);

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
}
