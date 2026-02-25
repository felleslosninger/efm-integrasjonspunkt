package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.ConversationStrategy;
import no.difi.meldingsutveksling.domain.EncryptedBusinessMessage;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.nextmove.nhn.NhnAdapterClient;
import no.difi.meldingsutveksling.nextmove.v2.NhnCryptoMessagePersister;
import no.difi.meldingsutveksling.nhn.adapter.model.EncryptedFagmelding;
import no.difi.meldingsutveksling.nhn.adapter.model.MessageOut;
import no.difi.meldingsutveksling.nhn.adapter.model.Receiver;
import no.difi.meldingsutveksling.nhn.adapter.model.Sender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
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
    private final NhnCryptoMessagePersister fileRepository;


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


            log.info("Attempt to send dialogmelding to nhn-adapter {}", message.getMessageId());
            String senderHerId1 = getHerID(message, ScopeType.SENDER_HERID1, "Sender HERID1 is not available");
            String senderHerId2 = getHerID(message, ScopeType.SENDER_HERID2, "Sender HERID2 is not available");
            String receiverHerId1 = getHerID(message, ScopeType.RECEIVER_HERID1, "Receiver HERID1 is not available");
            String receiverHerId2 = getHerID(message, ScopeType.RECEIVER_HERID2, "Receiver HERID2 is not available");
            EncryptedFagmelding dialogmelding = message.getBusinessMessage(EncryptedBusinessMessage.class).map(t-> new EncryptedFagmelding(t.getBase64DerEncryptionCertificate(),t.getMessage())).orElseThrow();

            Conversation conversation = conversationService.findConversation(message.getMessageId()).orElseThrow(() -> new NextMoveRuntimeException("Conversation not found for message " + message.getMessageId()));
            NhnIdentifier nhnIdentifier = (NhnIdentifier) message.getReceiver();

            MessageOut.Unsigned messageOut = new MessageOut.Unsigned(message.getMessageId(), message.getConversationId(), message.getSender().getIdentifier(),
                new Sender(senderHerId1, senderHerId2, "To Do"), new Receiver(receiverHerId1, receiverHerId2, nhnIdentifier.isFastlegeIdentifier() ? nhnIdentifier.getIdentifier() : null), dialogmelding, base64EncodedVedleg);
            var messageReference = adapterClient.messageOut(messageOut);
            conversation.setMessageReference(messageReference);
            conversationService.save(conversation);
        }
        catch (NextMoveException e) {
            throw e;
        }
        catch(Exception e) {
            log.error("Error during conversation for message {}", message.getMessageId(), e);
            throw new NextMoveException("Not able to send in melding over nhn for " + message.getMessageId(),e);
        }
    }
}
