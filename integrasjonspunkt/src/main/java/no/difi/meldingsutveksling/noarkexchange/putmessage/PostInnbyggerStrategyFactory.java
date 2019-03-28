package no.difi.meldingsutveksling.noarkexchange.putmessage;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.KeystoreProvider;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;

@RequiredArgsConstructor
public class PostInnbyggerStrategyFactory implements MessageStrategyFactory {

    private final DigitalPostInnbyggerConfig clientConfig;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final KeystoreProvider keystoreProvider;

    @Override
    public MessageStrategy create(Object payload) {
        return new PostInnbyggerMessageStrategy(clientConfig, serviceRegistryLookup, keystoreProvider.getKeyStore());
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPI;
    }

    public static MessageStrategyFactory newInstance(IntegrasjonspunktProperties properties, ServiceRegistryLookup serviceRegistryLookup, KeystoreProvider keystoreProvider) throws MeldingsformidlerException {
        return new PostInnbyggerStrategyFactory(properties.getDpi(), serviceRegistryLookup, keystoreProvider);
    }
}
