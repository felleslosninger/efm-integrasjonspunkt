package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.lang.StringUtils;

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
        postVirksomhetStrategyFactory = new PostVirksomhetStrategyFactory();

    }

    public MessageStrategyFactory getFactory(ServiceRecord serviceRecord) {
        Optional.ofNullable(serviceRecord).orElseThrow(() -> new IllegalArgumentException("serviceRecord cannot be null"));
        if (isEmpty(serviceRecord.getServiceIdentifier())) {
            throw new IllegalArgumentException("serviceRecord is missing a serviceIdentifier");
        }

        if ("EDU".equalsIgnoreCase(serviceRecord.getServiceIdentifier())) {
            return eduMessageStrategyFactory;
        } else {
            return postVirksomhetStrategyFactory;
        }

    }

}
