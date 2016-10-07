package no.difi.meldingsutveksling;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Class responsible for accessing the keystore for the Integrasjonspunkt.
 *
 * @author Glebnn Bech
 */
@Component
public class IntegrasjonspunktNokkel {

    private final IntegrasjonspunktProperties properties;

    @Autowired
    public IntegrasjonspunktNokkel(IntegrasjonspunktProperties properties) {
        this.properties = properties;
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
            keystore.load(i, properties.getCert().getPassword().toCharArray());

            Enumeration aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                boolean isKey = keystore.isKeyEntry(alias);
                if (isKey && alias.equals(properties.getCert().getAlias())) {
                    key = (PrivateKey) keystore.getKey(alias, properties.getCert().getPassword().toCharArray());
                }
            }
            if (key == null) {
                throw new MeldingsUtvekslingRuntimeException("no key with alias " + properties.getCert().getAlias() + " found in the keystore " + properties.getCert().getPath());
            }
            return key;
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public KeyPair getKeyPair() {

        KeyPair result = null;
        try (InputStream i = openKeyInputStream()) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(i, properties.getCert().getPassword().toCharArray());
            Enumeration aliases = keystore.aliases();
            for (; aliases.hasMoreElements();) {
                String alias = (String) aliases.nextElement();
                boolean isKey = keystore.isKeyEntry(alias);
                if (isKey && alias.equals(properties.getCert().getAlias())) {
                    PrivateKey key = (PrivateKey) keystore.getKey(alias, properties.getCert().getPassword().toCharArray());
                    X509Certificate c = (X509Certificate) keystore.getCertificate(alias);
                    result = new KeyPair(c.getPublicKey(), key);
                    break;
                }
            }
            if (result == null) {
                throw new MeldingsUtvekslingRuntimeException("no key with alias " + properties.getCert().getAlias() + " found in the keystore " + properties.getCert().getPath());
            }
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return result;
    }

    public SignatureHelper getSignatureHelper() {
        try {
            InputStream keyInputStream = openKeyInputStream();
            return new SignatureHelper(keyInputStream, properties.getCert().getPassword(), properties.getCert().getAlias(), properties.getCert().getPassword());
        } catch (FileNotFoundException e) {
            throw new MeldingsUtvekslingRuntimeException("keystore " + properties.getCert().getPath() + " not found on file system.");
        }
    }

    private InputStream openKeyInputStream() throws FileNotFoundException {
        return new FileInputStream(properties.getCert().getPath());
    }

}
