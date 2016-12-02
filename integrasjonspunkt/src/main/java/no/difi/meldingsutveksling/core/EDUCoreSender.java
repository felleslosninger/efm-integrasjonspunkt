package no.difi.meldingsutveksling.core;

import com.google.common.base.Strings;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MoveLogMarkers;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.noarkexchange.putmessage.MessageStrategy;
import no.difi.meldingsutveksling.noarkexchange.putmessage.MessageStrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.services.Adresseregister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EDUCoreSender {

    private static final Logger log = LoggerFactory.getLogger(EDUCoreSender.class);

    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final StrategyFactory strategyFactory;
    private final Adresseregister adresseRegister;
    private final NoarkClient mshClient;

    @Autowired
    public EDUCoreSender(IntegrasjonspunktProperties properties,
            ServiceRegistryLookup serviceRegistryLookup,
            StrategyFactory strategyFactory,
            Adresseregister adresseregister,
            NoarkClient mshClient) {
        this.properties = properties;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.strategyFactory = strategyFactory;
        this.adresseRegister = adresseregister;
        this.mshClient = mshClient;
    }

    public PutMessageResponseType sendMessage(EDUCore message) {
        MDC.put(MoveLogMarkers.KEY_ORGANISATION_NUMBER, properties.getOrg().getNumber());

        final ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(message.getReceiver().getIdentifier());
        final MessageStrategyFactory messageStrategyFactory = this.strategyFactory.getFactory(serviceRecord);
        PutMessageResponseType result;
        if (adresseRegister.hasAdresseregisterCertificate(message.getReceiver().getIdentifier())) {
            Audit.info("Receiver validated", EDUCoreMarker.markerFrom(message));

            MessageStrategy strategy = messageStrategyFactory.create(message.getPayload());
            result = strategy.send(message);
        } else if (!Strings.isNullOrEmpty(properties.getMsh().getEndpointURL())) {
            Audit.info("Send message to MSH", EDUCoreMarker.markerFrom(message));
            EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);
            result = mshClient.sendEduMelding(eduCoreFactory.createPutMessageFromCore(message));
        } else {
            Audit.error("Receiver not found", EDUCoreMarker.markerFrom(message));
            result = PutMessageResponseFactory.createErrorResponse(new MessageException(StatusMessage.UNABLE_TO_FIND_RECEIVER));
        }

        auditResult(result, message);
        return result;
    }

    private void auditResult(PutMessageResponseType result, EDUCore message) {
        if ("OK".equals(result.getResult().getType())) {
            Audit.info("Message sent", EDUCoreMarker.markerFrom(message));
        } else {
            Audit.error("Message sending failed", EDUCoreMarker.markerFrom(message));
        }
    }

}
