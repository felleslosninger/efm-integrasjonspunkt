package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.ConversationStrategy;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.jpa.ObjectMapperHolder;
import no.difi.meldingsutveksling.nextmove.nhn.DPHMessageOut;
import no.difi.meldingsutveksling.nextmove.nhn.NhnAdapterClient;
import no.difi.meldingsutveksling.nextmove.nhn.Reciever;
import no.difi.meldingsutveksling.nextmove.nhn.Sender;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Patient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.status.Conversation;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DphConversationStrategyImpl implements ConversationStrategy {


    private NhnAdapterClient adapterClient;
    private ServiceRegistryLookup serviceRegistryLookup;
    private ConversationService conversationService;

    public DphConversationStrategyImpl(NhnAdapterClient adapterClient, ServiceRegistryLookup serviceRegistryLookup, ConversationService conversationService) {
        this.adapterClient = adapterClient;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.conversationService = conversationService;
    }


    private String getHerID(NextMoveOutMessage message, ScopeType scopeType, String errorMessage) {
        return message.getSbd().getScope(scopeType).orElseThrow(() -> new RuntimeException(errorMessage)).getInstanceIdentifier();
    }

    @Override
    @Timed
    public void send(NextMoveOutMessage message) throws NextMoveException {
        log.info("Attempt to send dialogmelding to nhn-adapter");
        String senderHerId1 = getHerID(message, ScopeType.SENDER_HERID1, "Sender HERID1 is not available");
        String senderHerId2 = getHerID(message, ScopeType.SENDER_HERID2, "Sender HERID2 is not available");
        String recieverHerId1 = getHerID(message, ScopeType.RECEIVER_HERID1, "Reciever HERID1 is not available");
        String recieverHerId2 = getHerID(message, ScopeType.RECEIVER_HERID2, "Reciever HERID2 is not available");
        ServiceRecord receiverServiceRecord;
        Dialogmelding dialogmelding = message.getBusinessMessage(Dialogmelding.class).orElseThrow();
        try {
            var reciever = (NhnIdentifier) message.getReceiver();
            if (reciever.isFastlegeIdentifier()) {
                receiverServiceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(message.getReceiver().getIdentifier())
                    .conversationId(message.getSbd().getConversationId())
                    .process(message.getSbd().getProcess())
                    .build(), message.getSbd().getDocumentType());
                Person patient = new Person(receiverServiceRecord.getPatient().fnr(), receiverServiceRecord.getPatient().firstName(), receiverServiceRecord.getPatient().middleName(), receiverServiceRecord.getPatient().lastName(), "88888");
                dialogmelding.setPatient(patient);
                dialogmelding.setResponsibleHealthcareProfessionalId(reciever.getHerId2());

            }
            else {
                //@TODO If the message is NHN we should validate the patient in the validation phase.

                receiverServiceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(dialogmelding.getPatient().fnr())
                    .conversationId(message.getSbd().getConversationId())
                    .process(message.getSbd().getProcess())
                    .build(), message.getSbd().getDocumentType());

                Patient pat = receiverServiceRecord.getPatient();
                dialogmelding.setPatient(new Person(pat.fnr(), pat.firstName(),pat.middleName(),pat.lastName(),""));
                if (dialogmelding.getResponsibleHealthcareProfessionalId() == null) {
                    dialogmelding.setResponsibleHealthcareProfessionalId(reciever.getHerId2());
                }
            }


            //@TODO skal vi sende dialogmeldingen til og med at vi har ikke hentet patient detaliene ? Fortsatt vi har patient fødselsnummer kanskje det er nok ?
        } catch (Exception e) {
            log.error("Not able to get information about Person " + e.getMessage(), e);
        }
        String fagmelding = "";

        try {
            fagmelding = ObjectMapperHolder.get().writeValueAsString(dialogmelding);
        } catch (JsonProcessingException e) {
            throw new NextMoveException(e);
        }

        Conversation conversation = conversationService.findConversation(message.getMessageId()).orElseThrow(() -> new NextMoveRuntimeException("Conversation not found for message " + message.getMessageId()));
        NhnIdentifier nhnIdentifier = (NhnIdentifier) message.getReceiver();

        DPHMessageOut messageOut = new DPHMessageOut(message.getMessageId(), message.getConversationId(), message.getSender().getIdentifier(),
            new Sender(senderHerId1, senderHerId2, "To Do"),  new Reciever(recieverHerId1, recieverHerId2 , nhnIdentifier.isFastlegeIdentifier() ? nhnIdentifier.getIdentifier() : null), fagmelding);
        var messageReference = adapterClient.messageOut(messageOut);
        conversation.setMessageReference(messageReference);
        conversationService.save(conversation);
    }
}
