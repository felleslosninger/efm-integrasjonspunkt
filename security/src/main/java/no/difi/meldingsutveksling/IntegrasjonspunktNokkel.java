package no.difi.meldingsutveksling;

import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

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
            keystore.load(i, properties.getOrg().getKeystore().getPassword().toCharArray());

            Enumeration aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                boolean isKey = keystore.isKeyEntry(alias);
                if (isKey && alias.equals(properties.getOrg().getKeystore().getAlias())) {
                    key = (PrivateKey) keystore.getKey(alias, properties.getOrg().getKeystore().getPassword().toCharArray());
                }
            }
            if (key == null) {
                throw new MeldingsUtvekslingRuntimeException("no key with alias " + properties.getOrg().getKeystore().getAlias() + " found in the keystore " + properties.getOrg().getKeystore().getPath());
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
            keystore.load(i, properties.getOrg().getKeystore().getPassword().toCharArray());
            Enumeration aliases = keystore.aliases();
            for (; aliases.hasMoreElements();) {
                String alias = (String) aliases.nextElement();
                boolean isKey = keystore.isKeyEntry(alias);
                if (isKey && alias.equals(properties.getOrg().getKeystore().getAlias())) {
                    PrivateKey key = (PrivateKey) keystore.getKey(alias, properties.getOrg().getKeystore().getPassword().toCharArray());
                    X509Certificate c = (X509Certificate) keystore.getCertificate(alias);
                    result = new KeyPair(c.getPublicKey(), key);
                    break;
                }
            }
            if (result == null) {
                throw new MeldingsUtvekslingRuntimeException("no key with alias " + properties.getOrg().getKeystore().getAlias() + " found in the keystore " + properties.getOrg().getKeystore().getPath());
            }
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return result;
    }

    public X509Certificate getX509Certificate() {

        X509Certificate result = null;
        try (InputStream i = openKeyInputStream()) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(i, properties.getOrg().getKeystore().getPassword().toCharArray());
            Enumeration aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                boolean isKey = keystore.isKeyEntry(alias);
                if (isKey && alias.equals(properties.getOrg().getKeystore().getAlias())) {
                    result = (X509Certificate) keystore.getCertificate(alias);
                    break;
                }
            }
            if (result == null) {
                throw new MeldingsUtvekslingRuntimeException("no key with alias " + properties.getOrg().getKeystore().getAlias() + " found in the keystore " + properties.getOrg().getKeystore().getPath());
            }
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return result;
    }

    public SignatureHelper getSignatureHelper() {
        try {
            InputStream keyInputStream = openKeyInputStream();
            return new SignatureHelper(keyInputStream, properties.getOrg().getKeystore().getPassword(), properties.getOrg().getKeystore().getAlias(), properties.getOrg().getKeystore().getPassword());
        } catch (IOException e) {
            throw new MeldingsUtvekslingRuntimeException("keystore " + properties.getOrg().getKeystore().getPath() + " not found on file system.");
        }
    }

    private InputStream openKeyInputStream() throws IOException {
        return new FileInputStream(properties.getOrg().getKeystore().getPath().getFile());
    }

}
