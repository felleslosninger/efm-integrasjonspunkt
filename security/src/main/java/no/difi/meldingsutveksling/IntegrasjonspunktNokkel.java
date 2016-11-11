package no.difi.meldingsutveksling;

import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

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
public class IntegrasjonspunktNokkel {

    private final IntegrasjonspunktProperties.Keystore keystore;

    public IntegrasjonspunktNokkel(IntegrasjonspunktProperties.Keystore keystore) {
        this.keystore = keystore;
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
            keystore.load(i, this.keystore.getPassword().toCharArray());

            Enumeration aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                boolean isKey = keystore.isKeyEntry(alias);
                if (isKey && alias.equals(this.keystore.getAlias())) {
                    key = (PrivateKey) keystore.getKey(alias, this.keystore.getPassword().toCharArray());
                }
            }
            if (key == null) {
                throw new RuntimeException("no key with alias " + this.keystore.getAlias() + " found in the keystore " +
                        this.keystore.getPath());
            }
            return key;
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public KeyPair getKeyPair() {

        KeyPair result = null;
        try (InputStream i = openKeyInputStream()) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(i, this.keystore.getPassword().toCharArray());
            Enumeration aliases = keystore.aliases();
            for (; aliases.hasMoreElements();) {
                String alias = (String) aliases.nextElement();
                boolean isKey = keystore.isKeyEntry(alias);
                if (isKey && alias.equals(this.keystore.getAlias())) {
                    PrivateKey key = (PrivateKey) keystore.getKey(alias, this.keystore.getPassword().toCharArray());
                    X509Certificate c = (X509Certificate) keystore.getCertificate(alias);
                    result = new KeyPair(c.getPublicKey(), key);
                    break;
                }
            }
            if (result == null) {
                throw new RuntimeException("no key with alias " + this.keystore.getAlias() + " found in the keystore " +
                        this.keystore.getPath());
            }
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public X509Certificate getX509Certificate() {

        X509Certificate result = null;
        try (InputStream i = openKeyInputStream()) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(i, this.keystore.getPassword().toCharArray());
            Enumeration aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                boolean isKey = keystore.isKeyEntry(alias);
                if (isKey && alias.equals(this.keystore.getAlias())) {
                    result = (X509Certificate) keystore.getCertificate(alias);
                    break;
                }
            }
            if (result == null) {
                throw new RuntimeException("no key with alias " + this.keystore.getAlias() + " found in the keystore " +
                        this.keystore.getPath());
            }
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public SignatureHelper getSignatureHelper() {
        try {
            InputStream keyInputStream = openKeyInputStream();
            return new SignatureHelper(keyInputStream, this.keystore.getPassword(), this.keystore.getAlias(), this
                    .keystore.getPassword());
        } catch (IOException e) {
            throw new RuntimeException("keystore " + this.keystore.getPath() + " not found on file system.");
        }
    }

    private InputStream openKeyInputStream() throws IOException {
        return new FileInputStream(this.keystore.getPath().getFile());
    }

}
