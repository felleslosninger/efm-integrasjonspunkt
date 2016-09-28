package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ptp.MeldingsformidlerClient;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerException;
import org.springframework.core.env.Environment;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class PostInnbyggerStrategyFactory implements MessageStrategyFactory {

    private final MeldingsformidlerClient.Config clientConfig;

    public PostInnbyggerStrategyFactory(MeldingsformidlerClient.Config clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostInnbyggerMessageStrategy(clientConfig);
    }

    public static MessageStrategyFactory newInstance(Environment environment) throws MeldingsformidlerException {
        final String keystoreLocation = environment.getProperty("meldingsformidler.keystore.location");
        final String keystorePassword = environment.getProperty("meldingsformidler.keystore.password");
        final KeyStore keyStore = setupKeyStore(keystoreLocation, keystorePassword.toCharArray());
        final MeldingsformidlerClient.Config clientConfig = MeldingsformidlerClient.Config.from(environment, keyStore);
        return new PostInnbyggerStrategyFactory(clientConfig);
    }

    private static KeyStore setupKeyStore(String filename, char[] password) throws MeldingsformidlerException {
        KeyStore keystore = null;
        try {
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            throw new MeldingsformidlerException("Could not initialize keystore", e);
        }
        try (FileInputStream file = new FileInputStream(filename)){
            keystore.load(file, password);
        } catch (IOException e) {
            throw new MeldingsformidlerException("Could not open keystore file", e);
        } catch (CertificateException | NoSuchAlgorithmException e) {
            throw new MeldingsformidlerException("Unable to load keystore file", e);
        }

        return keystore;
    }
}
