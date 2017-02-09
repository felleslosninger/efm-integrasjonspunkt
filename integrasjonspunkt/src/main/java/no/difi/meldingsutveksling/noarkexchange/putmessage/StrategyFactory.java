package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.lang.NotImplementedException;

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

        if (EDU.fullname().equalsIgnoreCase(serviceRecord.getServiceIdentifier())) {
            return eduMessageStrategyFactory;
        } else if (DPV.fullname().equalsIgnoreCase(serviceRecord.getServiceIdentifier())) {
            return postVirksomhetStrategyFactory;
        } else if (DPI.fullname().equalsIgnoreCase(serviceRecord.getServiceIdentifier())) {
            return postInnbyggerStrategyFactory;
        } else if (FIKS.fullname().equalsIgnoreCase(serviceRecord.getServiceIdentifier())) {
            return fiksMessageStrategyFactory;
        } else {
            throw new NotImplementedException(String.format("Integrasjonspunkt has no message strategy matching service identifier matching %s", serviceRecord.getServiceIdentifier()));
        }

    }

}
