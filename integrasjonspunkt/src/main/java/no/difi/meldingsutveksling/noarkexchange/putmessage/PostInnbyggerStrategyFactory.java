package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ptp.MeldingsformidlerClient;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.core.env.Environment;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class PostInnbyggerStrategyFactory implements MessageStrategyFactory {

    private final MeldingsformidlerClient.Config clientConfig;
    private final ServiceRegistryLookup serviceRegistryLookup;

    public PostInnbyggerStrategyFactory(MeldingsformidlerClient.Config clientConfig, ServiceRegistryLookup serviceRegistryLookup) {
        this.clientConfig = clientConfig;
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostInnbyggerMessageStrategy(clientConfig, serviceRegistryLookup);
    }

    public static MessageStrategyFactory newInstance(Environment environment, ServiceRegistryLookup serviceRegistryLookup) throws MeldingsformidlerException {
        final String keystoreLocation = environment.getProperty("meldingsformidler.keystore.location");
        final String keystorePassword = environment.getProperty("meldingsformidler.keystore.password");
        final KeyStore keyStore = setupKeyStore(keystoreLocation, keystorePassword.toCharArray());
        final MeldingsformidlerClient.Config clientConfig = MeldingsformidlerClient.Config.from(environment, keyStore);
        return new PostInnbyggerStrategyFactory(clientConfig, serviceRegistryLookup);
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
