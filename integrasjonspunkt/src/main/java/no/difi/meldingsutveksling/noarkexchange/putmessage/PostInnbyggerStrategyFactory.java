package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ptp.MeldingsformidlerClient;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.core.env.Environment;

public class PostInnbyggerStrategyFactory implements MessageStrategyFactory {

    private final MeldingsformidlerClient.Config clientConfig;
    private final ServiceRegistryLookup serviceRegistryLookup;

    private PostInnbyggerStrategyFactory(MeldingsformidlerClient.Config clientConfig, ServiceRegistryLookup serviceRegistryLookup, KeystoreProvider keystoreProvider) {
        this.clientConfig = clientConfig;
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostInnbyggerMessageStrategy(clientConfig, serviceRegistryLookup);
    }

    public static MessageStrategyFactory newInstance(Environment environment, ServiceRegistryLookup serviceRegistryLookup, KeystoreProvider keystoreProvider) throws MeldingsformidlerException {
        final MeldingsformidlerClient.Config clientConfig = MeldingsformidlerClient.Config.from(environment, keystoreProvider.getKeyStore());
        return new PostInnbyggerStrategyFactory(clientConfig, serviceRegistryLookup, keystoreProvider);
    }


}
