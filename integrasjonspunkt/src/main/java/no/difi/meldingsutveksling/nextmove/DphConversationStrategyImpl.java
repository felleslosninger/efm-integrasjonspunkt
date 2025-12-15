package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.ConversationStrategy;
import no.difi.meldingsutveksling.api.CryptoMessagePersister;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.jpa.ObjectMapperHolder;
import no.difi.meldingsutveksling.nextmove.nhn.DPHMessageOut;
import no.difi.meldingsutveksling.nextmove.nhn.NhnAdapterClient;
import no.difi.meldingsutveksling.nextmove.nhn.Receiver;
import no.difi.meldingsutveksling.nextmove.nhn.Sender;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Patient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.status.Conversation;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class DphConversationStrategyImpl implements ConversationStrategy {


    private final NhnAdapterClient adapterClient;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final ConversationService conversationService;
    private final CryptoMessagePersister fileRepository;


    private String getHerID(NextMoveOutMessage message, ScopeType scopeType, String errorMessage) {
        return message.getSbd().getScope(scopeType).orElseThrow(() -> new NextMoveRuntimeException(errorMessage)).getInstanceIdentifier();
    }

    @Override
    @Timed
    public void send(NextMoveOutMessage message) throws NextMoveException {
        try {
            var filename = message.getFiles().stream().findFirst().map(BusinessMessageFile::getIdentifier).orElseThrow(()->new NextMoveException("Filename can not be null. "));
            byte vedlegInnehold[];
            String base64EncodedVedleg;

            try {
                Resource vedlegg = fileRepository.read(message.getMessageId(), filename);
                vedlegInnehold = vedlegg.getContentAsByteArray();
            } catch (IOException e) {
                throw new NextMoveException("Can not postprocess file.",e);
            } catch (Exception e) {
                throw new NextMoveException("Can not read file " + filename, e);
            }



            base64EncodedVedleg = Base64.getEncoder().encodeToString(vedlegInnehold);


            log.info("Attempt to send dialogmelding to nhn-adapter");
            String senderHerId1 = getHerID(message, ScopeType.SENDER_HERID1, "Sender HERID1 is not available");
            String senderHerId2 = getHerID(message, ScopeType.SENDER_HERID2, "Sender HERID2 is not available");
            String receiverHerId1 = getHerID(message, ScopeType.RECEIVER_HERID1, "Receiver HERID1 is not available");
            String receiverHerId2 = getHerID(message, ScopeType.RECEIVER_HERID2, "Receiver HERID2 is not available");
            ServiceRecord receiverServiceRecord;
            var reciever = (NhnIdentifier) message.getReceiver();
            Dialogmelding dialogmelding = message.getBusinessMessage(Dialogmelding.class).orElseThrow();
            DialogmeldingOut.DialogmeldingOutBuilder outMessageBuilder = DialogmeldingOut.builder()
                .notat(dialogmelding.getNotat())
                .vedleggBeskrivelse(dialogmelding.getVedleggBeskrivelse())
                .responsibleHealthcareProfessionalId(reciever.getHerId2());
            try {

                if (reciever.isFastlegeIdentifier()) {
                    receiverServiceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(message.getReceiver().getIdentifier())
                        .conversationId(message.getSbd().getConversationId())
                        .process(message.getSbd().getProcess())
                        .build(), message.getSbd().getDocumentType());


                    Person patient = new Person(receiverServiceRecord.getPatient().fnr(), receiverServiceRecord.getPatient().firstName(), receiverServiceRecord.getPatient().middleName(), receiverServiceRecord.getPatient().lastName(), "88888");
                    outMessageBuilder
                        .patient(patient);

                } else {
                    //@TODO If the message is NHN we should validate the patient in the validation phase.
                    // vi trenger å sikre at sånn fødselsnummer eksisterer.

                    receiverServiceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(dialogmelding.getPatientFnr())
                        .conversationId(message.getSbd().getConversationId())
                        .process(message.getSbd().getProcess())
                        .build(), message.getSbd().getDocumentType());
                    Patient pat = receiverServiceRecord.getPatient();
                    if (dialogmelding.getResponsibleHealthcareProfessionalId() == null) {
                        outMessageBuilder.responsibleHealthcareProfessionalId(reciever.getHerId2());
                    }
                    outMessageBuilder.patient(new Person(pat.fnr(), pat.firstName(), pat.middleName(), pat.lastName(), ""));


                }

            } catch (Exception e) {
                log.error("Not able to get information about Person {} for {}", e.getMessage(), message.getMessageId(), e);
                throw new NextMoveException("Not able to get information about Person " + e.getMessage(), e);
            }
            String fagmelding = "";

            try {
                fagmelding = ObjectMapperHolder.get().writeValueAsString(outMessageBuilder.build());
            } catch (JsonProcessingException e) {
                throw new NextMoveException(e);
            }

            Conversation conversation = conversationService.findConversation(message.getMessageId()).orElseThrow(() -> new NextMoveRuntimeException("Conversation not found for message " + message.getMessageId()));
            NhnIdentifier nhnIdentifier = (NhnIdentifier) message.getReceiver();

            DPHMessageOut messageOut = new DPHMessageOut(message.getMessageId(), message.getConversationId(), message.getSender().getIdentifier(),
                new Sender(senderHerId1, senderHerId2, "To Do"), new Receiver(receiverHerId1, receiverHerId2, nhnIdentifier.isFastlegeIdentifier() ? nhnIdentifier.getIdentifier() : null), fagmelding, base64EncodedVedleg);
            var messageReference = adapterClient.messageOut(messageOut);
            conversation.setMessageReference(messageReference);
            conversationService.save(conversation);
        }
        catch (NextMoveException e) {
            throw e;
        }
        catch(Exception e) {
            throw new NextMoveException("Not able to send in melding over nhn for " + message.getMessageId(),e);
        }
    }
}
