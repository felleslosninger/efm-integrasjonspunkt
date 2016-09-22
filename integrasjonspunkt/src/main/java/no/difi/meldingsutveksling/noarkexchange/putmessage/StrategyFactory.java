package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.lang.NotImplementedException;

import java.util.Optional;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * PutMessages are delivered based on their ServiceRecord as returned from ServiceRegistry
 */
public class StrategyFactory {
    private final EduMessageStrategyFactory eduMessageStrategyFactory;
    private final PostVirksomhetStrategyFactory postVirksomhetStrategyFactory;

    public StrategyFactory(MessageSender messageSender) {
        eduMessageStrategyFactory = EduMessageStrategyFactory.newInstance(messageSender);
        postVirksomhetStrategyFactory = PostVirksomhetStrategyFactory.newInstance(messageSender.getEnvironment());

    }

    /**
     * Use to get appropriate Message Strategy Factory: if the service record specifies Edu then a factory for that
     * medium will be returned
     * @param serviceRecord
     * @return factory to send messages corresponding to provided service record
     */
    public MessageStrategyFactory getFactory(ServiceRecord serviceRecord) {
        Optional.ofNullable(serviceRecord).orElseThrow(() -> new IllegalArgumentException("serviceRecord cannot be null"));
        if (isEmpty(serviceRecord.getServiceIdentifier())) {
            throw new IllegalArgumentException("serviceRecord is missing a serviceIdentifier");
        }

        if ("EDU".equalsIgnoreCase(serviceRecord.getServiceIdentifier())) {
            return eduMessageStrategyFactory;
        } else if ("POST_VIRKSOMHET".equalsIgnoreCase(serviceRecord.getServiceIdentifier())) {
            return postVirksomhetStrategyFactory;
        } else {
            throw new NotImplementedException(String.format("Integrasjonspunkt has no message strategy matching service identifier matching %s", serviceRecord.getServiceIdentifier()));
        }

    }

}
