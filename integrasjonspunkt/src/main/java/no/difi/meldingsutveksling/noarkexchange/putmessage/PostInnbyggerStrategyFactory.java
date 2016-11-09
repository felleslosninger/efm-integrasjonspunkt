package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ptp.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;

public class PostInnbyggerStrategyFactory implements MessageStrategyFactory {

    private final DigitalPostInnbyggerConfig clientConfig;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private KeystoreProvider keystoreProvider;

    private PostInnbyggerStrategyFactory(DigitalPostInnbyggerConfig clientConfig, ServiceRegistryLookup serviceRegistryLookup, KeystoreProvider keystoreProvider) {
        this.clientConfig = clientConfig;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.keystoreProvider = keystoreProvider;
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostInnbyggerMessageStrategy(clientConfig, serviceRegistryLookup, keystoreProvider.getKeyStore());
    }

    public static MessageStrategyFactory newInstance(IntegrasjonspunktProperties properties, ServiceRegistryLookup serviceRegistryLookup, KeystoreProvider keystoreProvider) throws MeldingsformidlerException {
        return new PostInnbyggerStrategyFactory(properties.getDpi(), serviceRegistryLookup, keystoreProvider);
    }

}
