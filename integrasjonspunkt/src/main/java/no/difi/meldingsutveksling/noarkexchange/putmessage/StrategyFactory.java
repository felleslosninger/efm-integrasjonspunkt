package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.KeystoreProvider;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.apache.commons.lang.NotImplementedException;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import static no.difi.meldingsutveksling.ServiceIdentifier.*;

/**
 * PutMessages are delivered based on their ServiceRecord as returned from ServiceRegistry
 */
public class StrategyFactory {

    private final Map<ServiceIdentifier, MessageStrategyFactory> factories;

    public StrategyFactory(
            CorrespondenceAgencyClient client,
            CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory,
            MessageSender messageSender,
            ServiceRegistryLookup serviceRegistryLookup,
            KeystoreProvider keystoreProvider,
            IntegrasjonspunktProperties properties,
            NoarkClient noarkClient,
            InternalQueue internalQueue) {
        MessageStrategyFactory postInnbyggerStrategyFactory;
        try {
            postInnbyggerStrategyFactory = PostInnbyggerStrategyFactory.newInstance(properties, serviceRegistryLookup, keystoreProvider);
        } catch (MeldingsformidlerException e) {
            throw new MeldingsUtvekslingRuntimeException("Unable to create client for sikker digital post", e);
        }

        factories = new EnumMap<>(ServiceIdentifier.class);
        if (properties.getFeature().isEnableDPO()) {
            factories.put(DPO, EduMessageStrategyFactory.newInstance(messageSender));
        }
        if (properties.getFeature().isEnableDPI()) {
            factories.put(DPI, postInnbyggerStrategyFactory);
        }
        if (properties.getFeature().isEnableDPV()) {
            factories.put(DPV, PostVirksomhetStrategyFactory.newInstance(correspondenceAgencyMessageFactory, client, noarkClient, internalQueue));
            factories.put(DPE_INNSYN, PostVirksomhetStrategyFactory.newInstance(correspondenceAgencyMessageFactory, client, noarkClient, internalQueue));
        }
    }

    /**
     * Used with feature toggling to enable new message strategies
     *
     * @param messageStrategyFactory an implementation/instance of a MessageStrategyFactory
     */
    public void registerMessageStrategyFactory(MessageStrategyFactory messageStrategyFactory) {
        factories.put(messageStrategyFactory.getServiceIdentifier(), messageStrategyFactory);
    }

    /**
     * Use to get appropriate Message Strategy Factory: if the service record specifies Edu then a factory for that medium will be
     * returned
     *
     * @param si serviceIdentifier for the message
     * @return factory to send messages corresponding to provided service record
     */
    public MessageStrategyFactory getFactory(ServiceIdentifier si) {
        Objects.requireNonNull(si, "ServiceIdentifier cannot be null");
        final MessageStrategyFactory factory = factories.get(si);
        if (factory == null) {
            throw new NotImplementedException(String.format("Integrasjonspunkt has no message strategy matching service identifier matching %s", si));
        }
        return factory;
    }

    public boolean hasFactory(ServiceIdentifier serviceIdentifier) {
        return factories.containsKey(serviceIdentifier);
    }
}
