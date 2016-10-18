package no.difi.meldingsutveksling.core;

import com.google.common.base.Strings;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MoveLogMarkers;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
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

    private IntegrasjonspunktProperties properties;
    private ServiceRegistryLookup serviceRegistryLookup;
    private StrategyFactory strategyFactory;
    private Adresseregister adresseRegister;
    private NoarkClient mshClient;

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

    public boolean sendMessage(EDUCore message) {
        MDC.put(MoveLogMarkers.KEY_ORGANISATION_NUMBER, properties.getOrg().getNumber());

        final ServiceRecord primaryServiceRecord = serviceRegistryLookup.getPrimaryServiceRecord(message.getReceiver().getOrgNr());
        final MessageStrategyFactory messageStrategyFactory = this.strategyFactory.getFactory(primaryServiceRecord);
        boolean result;
        if (adresseRegister.hasAdresseregisterCertificate(message.getReceiver().getOrgNr())) {
            Audit.info("Receiver validated", EDUCoreMarker.markerFrom(message));

            MessageStrategy strategy = messageStrategyFactory.create(message.getPayload());
            PutMessageResponseType response = strategy.send(message);
            result = "OK".equals(response.getResult().getType());
        } else if (!Strings.isNullOrEmpty(properties.getMsh().getEndpointURL())) {
            Audit.info("Send message to MSH", EDUCoreMarker.markerFrom(message));
            EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);
            PutMessageResponseType response = mshClient.sendEduMelding(eduCoreFactory.createPutMessageFromCore(message));
            result = "OK".equals(response.getResult().getType());
        } else {
            Audit.error("Receiver not found", EDUCoreMarker.markerFrom(message));
            result = false;
        }
        if (result) {
            Audit.info("Message sent", EDUCoreMarker.markerFrom(message));
        } else {
            Audit.error("Message sending failed", EDUCoreMarker.markerFrom(message));
        }
        return result;
    }

}
