package no.difi.meldingsutveksling.noarkexchange.putmessage;

import com.google.common.collect.ImmutableMap;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.lang.NotImplementedException;

import java.util.Map;
import java.util.Optional;

import static no.difi.meldingsutveksling.ServiceIdentifier.*;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * PutMessages are delivered based on their ServiceRecord as returned from ServiceRegistry
 */
public class StrategyFactory {

    private final EduMessageStrategyFactory eduMessageStrategyFactory;
    private final PostVirksomhetStrategyFactory postVirksomhetStrategyFactory;
    private final MessageStrategyFactory postInnbyggerStrategyFactory;
    private final FiksMessageStrategyFactory fiksMessageStrategyFactory;
    private final Map<String, MessageStrategyFactory> factories;

    public StrategyFactory(MessageSender messageSender, ServiceRegistryLookup serviceRegistryLookup,
                           KeystoreProvider keystoreProvider, IntegrasjonspunktProperties properties) {
        eduMessageStrategyFactory = EduMessageStrategyFactory.newInstance(messageSender, properties);
        postVirksomhetStrategyFactory = PostVirksomhetStrategyFactory.newInstance(messageSender.getProperties(), serviceRegistryLookup);



        try {
            postInnbyggerStrategyFactory = PostInnbyggerStrategyFactory.newInstance(messageSender.getProperties(), serviceRegistryLookup, keystoreProvider);
        } catch (MeldingsformidlerException e) {
            throw new MeldingsUtvekslingRuntimeException("Unable to create client for sikker digital post", e);
        }

        fiksMessageStrategyFactory = FiksMessageStrategyFactory.newInstance();
        factories = ImmutableMap.<String, MessageStrategyFactory>builder()
                .put(EDU.fullname(), eduMessageStrategyFactory)
                .put(DPI.fullname(), postInnbyggerStrategyFactory)
                .put(DPV.fullname(), postVirksomhetStrategyFactory)
                .put(FIKS.fullname(), fiksMessageStrategyFactory)
                .build();
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
        if (isEmpty(serviceRecord.getServiceIdentifier())) {
            throw new IllegalArgumentException("serviceRecord is missing a serviceIdentifier");
        }
        final MessageStrategyFactory factory = factories.get(serviceRecord.getServiceIdentifier());
        if (factory == null) {
            throw new NotImplementedException(String.format("Integrasjonspunkt has no message strategy matching service identifier matching %s", serviceRecord.getServiceIdentifier()));
        }
        return factory;
    }

}
