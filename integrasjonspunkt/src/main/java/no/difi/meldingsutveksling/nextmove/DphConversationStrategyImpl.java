package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationStrategy;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.nhn.*;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryClient;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Slf4j
@Component
public class DphConversationStrategyImpl implements ConversationStrategy {


    private NhnAdapterClient adapterClient;
    private ServiceRegistryLookup serviceRegistryLookup;
    private ServiceRegistryClient serviceRegistryClient;

    public DphConversationStrategyImpl(NhnAdapterClient adapterClient, ServiceRegistryLookup serviceRegistryLookup) {
        this.adapterClient = adapterClient;
        this.serviceRegistryLookup = serviceRegistryLookup;
    }


    private String getHerID(NextMoveOutMessage message, String scope, String errorMessage) {
        return message.getSbd().getScopes().stream().filter(t ->
            Objects.equals(t.getType(), scope)).findFirst().orElseThrow(() ->
            new RuntimeException(errorMessage)
        ).getIdentifier();
    }

    @Override
    @Timed
    public void send(NextMoveOutMessage message) throws NextMoveException {
        log.info("Attempt to send dialogmelding to nhn-adapter");
        String senderHerId1 = getHerID(message, "SENDER_HERID1", "Sender HERID1 is not available");
        String senderHerId2 = getHerID(message, "SENDER_HERID2", "Sender HERID2 is not available");
        String recieverHerId1 = getHerID(message, "RECIEVER_HERID1", "Reciever HERID1 is not available");
        String recieverHerId2 = getHerID(message, "RECIEVER_HERID2", "Reciever HERID2 is not available");
        Patient patient = null;
        try {
            var serviceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                .conversationId(message.getSbd().getConversationId())
                .process(message.getSbd().getProcess())
                .build(), message.getSbd().getType());
            patient = new Patient(serviceRecord.getPatient().fnr(), serviceRecord.getPatient().firstName(), serviceRecord.getPatient().middleName(), serviceRecord.getPatient().lastName(), "88888");

            System.out.println(serviceRecord.getService());
            //@TODO skal vi sende dialogmeldingen til og med at vi har ikke hentet patient detaliene ? Fortsatt vi har patient fødselsnummer kanskje det er nok ?
        } catch (Exception e) {
           log.error("Not able to get information about Patient " + e.getMessage(), e);
        }

        DPHMessageOut messageOut = new DPHMessageOut(message.getMessageId(), message.getConversationId(), message.getSenderIdentifier(),
            new Sender(senderHerId1, senderHerId2), new Reciever(recieverHerId1, recieverHerId2), "fagmelding", patient);
        adapterClient.messageOut(messageOut);


    }
}
