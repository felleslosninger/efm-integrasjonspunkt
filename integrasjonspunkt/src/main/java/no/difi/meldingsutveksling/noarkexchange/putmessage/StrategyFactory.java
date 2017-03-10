package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.lang.NotImplementedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static no.difi.meldingsutveksling.ServiceIdentifier.*;

/**
 * PutMessages are delivered based on their ServiceRecord as returned from ServiceRegistry
 */
public class StrategyFactory {

    private final Map<ServiceIdentifier, MessageStrategyFactory> factories;

    public StrategyFactory(MessageSender messageSender, ServiceRegistryLookup serviceRegistryLookup, KeystoreProvider keystoreProvider, IntegrasjonspunktProperties properties) {
        MessageStrategyFactory postInnbyggerStrategyFactory;
        try {
            postInnbyggerStrategyFactory = PostInnbyggerStrategyFactory.newInstance(messageSender.getProperties(), serviceRegistryLookup, keystoreProvider);
        } catch (MeldingsformidlerException e) {
            throw new MeldingsUtvekslingRuntimeException("Unable to create client for sikker digital post", e);
        }

        factories = new HashMap<>();
        factories.put(DPO, EduMessageStrategyFactory.newInstance(messageSender, properties));
        factories.put(DPI, postInnbyggerStrategyFactory);
        factories.put(DPV, PostVirksomhetStrategyFactory.newInstance(messageSender.getProperties(), serviceRegistryLookup));
    }

    /**
     * Used with feature toggling to enable new message strategies
     * @param messageStrategyFactory an implementation/instance of a MessageStrategyFactory
     */
    public void registerMessageStrategyFactory(MessageStrategyFactory messageStrategyFactory) {
        factories.put(messageStrategyFactory.getServiceIdentifier(), messageStrategyFactory);
    }

    /**
     * Use to get appropriate Message Strategy Factory: if the service record specifies Edu then a factory for that medium will be
     * returned
     *
     * @param serviceRecord as returned from the Service Registry service
     * @return factory to send messages corresponding to provided service record
     */
    public MessageStrategyFactory getFactory(ServiceRecord serviceRecord) {
        Optional.ofNullable(serviceRecord).orElseThrow(() -> new IllegalArgumentException("serviceRecord cannot be null"));
        if (serviceRecord.getServiceIdentifier() == null) {
            throw new IllegalArgumentException("serviceRecord is missing a serviceIdentifier");
        }
        final MessageStrategyFactory factory = factories.get(serviceRecord.getServiceIdentifier());
        if (factory == null) {
            throw new NotImplementedException(String.format("Integrasjonspunkt has no message strategy matching service identifier matching %s", serviceRecord.getServiceIdentifier()));
        }
        return factory;
    }

    public boolean hasFactory(ServiceIdentifier serviceIdentifier) {
        return factories.containsKey(serviceIdentifier);
    }
}
