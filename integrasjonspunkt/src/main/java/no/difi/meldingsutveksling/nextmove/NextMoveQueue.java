package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.Decryptor;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;
import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
public class NextMoveQueue {

    private static final Logger log = LoggerFactory.getLogger(NextMoveQueue.class);

    private DirectionalConversationResourceRepository inRepo;
    private IntegrasjonspunktNokkel keyInfo;
    private ConversationService conversationService;
    private NextMoveMessageInRepository messageRepo;

    public NextMoveQueue(ConversationResourceRepository repo,
                         IntegrasjonspunktNokkel keyInfo,
                         ConversationService conversationService,
                         NextMoveMessageInRepository messageRepo) {
        inRepo = new DirectionalConversationResourceRepository(repo, INCOMING);
        this.keyInfo = keyInfo;
        this.conversationService = conversationService;
        this.messageRepo = messageRepo;
    }

    public Optional<NextMoveMessage> enqueue(StandardBusinessDocument sbd) {
        if (sbd.getAny() instanceof BusinessMessage) {
            NextMoveInMessage message = NextMoveInMessage.of(sbd);

            if (ServiceIdentifier.DPE_RECEIPT.equals(message.getServiceIdentifier())) {
                handleDpeReceipt(message.getConversationId());
                return Optional.empty();
            }

            messageRepo.findByConversationId(sbd.getConversationId())
                    .orElseGet(() -> messageRepo.save(message));

            Conversation c = conversationService.registerConversation(message);
            conversationService.registerStatus(c, MessageStatus.of(GenericReceiptStatus.INNKOMMENDE_MOTTATT));
            Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] put on local queue",
                    message.getConversationId(), message.getServiceIdentifier()), markerFrom(message));
            return Optional.of(message);

        } else {
            String errorMsg = String.format("SBD payload not of known types: %s, %s", Payload.class.getName(), BusinessMessage.class.getName());
            log.error(errorMsg);
            throw new MeldingsUtvekslingRuntimeException(errorMsg);
        }
    }

    public Optional<ConversationResource> enqueueSBDFromCR(StandardBusinessDocument sbd) {
        if (sbd.getAny() instanceof Payload) {
            Payload payload = (Payload) sbd.getAny();
            ConversationResource message = (payload).getConversation();

            if (ServiceIdentifier.DPE_RECEIPT.equals(message.getServiceIdentifier())) {
                handleDpeReceipt(message.getConversationId());
                return Optional.empty();
            }

            message = inRepo.save(message);

            Conversation c = conversationService.registerConversation(message);
            conversationService.registerStatus(c, MessageStatus.of(GenericReceiptStatus.INNKOMMENDE_MOTTATT));
            Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] put on local queue",
                    message.getConversationId(), message.getServiceIdentifier()), markerFrom(message));
            return Optional.of(message);

        } else {
            String errorMsg = String.format("SBD payload not of known types: %s, %s", Payload.class.getName(), BusinessMessage.class.getName());
            log.error(errorMsg);
            throw new MeldingsUtvekslingRuntimeException(errorMsg);
        }
    }

    private void handleDpeReceipt(String conversationId) {
        log.debug(String.format("Message with id=%s is a receipt", conversationId));
        Optional<Conversation> c = conversationService.registerStatus(conversationId, MessageStatus.of(GenericReceiptStatus.LEVERT));
        c.ifPresent(conversationService::markFinished);
    }

    public byte[] decrypt(Payload payload) {
        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        return new Decryptor(keyInfo).decrypt(cmsEncZip);
    }

    public List<String> getContentFromAsic(byte[] bytes) throws MessageException {
        List<String> files = Lists.newArrayList();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                files.add(entry.getName());
            }
        } catch (Exception e) {
            log.error("Failed reading entries in asic.", e);
            throw new MessageException(StatusMessage.UNABLE_TO_EXTRACT_ZIP_CONTENTS);
        }
        return files;
    }

}
