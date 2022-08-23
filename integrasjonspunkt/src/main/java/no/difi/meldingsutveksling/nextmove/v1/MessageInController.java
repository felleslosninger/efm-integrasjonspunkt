package no.difi.meldingsutveksling.nextmove.v1;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.AsicUtils;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.CryptoMessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.FileNotFoundException;
import no.difi.meldingsutveksling.exceptions.MessageNotFoundException;
import no.difi.meldingsutveksling.exceptions.MessageNotLockedException;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveInMessageQueryInput;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInService;
import no.difi.meldingsutveksling.nextmove.v2.PageRequests;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@RestController
@RequestMapping("/in/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageInController {

    private static final MediaType MIMETYPE_ASICE = MediaType.parseMediaType(AsicUtils.MIMETYPE_ASICE);
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String HEADER_FILENAME = "attachment; filename=";

    private final NextMoveMessageInRepository inRepo;
    private final Clock clock;
    private final IntegrasjonspunktProperties props;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final ConversationService conversationService;
    private final InternalQueue internalQueue;
    private final SBDFactory sbdFactory;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final NextMoveMessageInService messageService;

    @GetMapping("/peek")
    public ResponseEntity peek(@RequestParam(value = "serviceIdentifier", required = false) String serviceIdentifier) {

        NextMoveInMessageQueryInput query = new NextMoveInMessageQueryInput().setServiceIdentifier("DPE");

        NextMoveInMessage message = messageService.peek(query)
                .orElseThrow(NoContentException::new);

        log.info(markerFrom(message), "Conversation with id={} locked", message.getMessageId());

        Map<String, String> customProperties = Maps.newHashMap();
        String si = null;
        if (message.getBusinessMessage() instanceof InnsynskravMessage) {
            si = "DPE_INNSYN";
            InnsynskravMessage innsynskravMessage = (InnsynskravMessage) message.getBusinessMessage();
            customProperties.put("orgnumber", innsynskravMessage.getOrgnr());
            customProperties.put("epost", innsynskravMessage.getEpost());
        }
        if (message.getBusinessMessage() instanceof PubliseringMessage) {
            si = "DPE_DATA";
            PubliseringMessage publiseringMessage = (PubliseringMessage) message.getBusinessMessage();
            customProperties.put("orgnumber", publiseringMessage.getOrgnr());
        }

        Map<Integer, String> fileRefs = Maps.newHashMap();
        fileRefs.put(0, ASIC_FILE);

        InfoRecord senderInfoRecord = serviceRegistryLookup.getInfoRecord(message.getSender().getPrimaryIdentifier());
        InfoRecord receiverInfoRecord = serviceRegistryLookup.getInfoRecord(message.getReceiver().getPrimaryIdentifier());

        NextMoveV1Message peekMessage = new NextMoveV1Message()
                .setConversationId(message.getMessageId())
                .setSenderId(message.getSenderIdentifier())
                .setReceiverId(message.getReceiverIdentifier())
                .setSenderName(senderInfoRecord.getOrganizationName())
                .setReceiverName(receiverInfoRecord.getOrganizationName())
                .setServiceIdentifier(si)
                .setLastUpdate(LocalDateTime.now())
                .setFileRefs(fileRefs)
                .setCustomProperties(customProperties);
        return ResponseEntity.ok(peekMessage);
    }

    @PostMapping("/pop")
    @Transactional
    public ResponseEntity popPost(
            @RequestParam(value = "serviceIdentifier", required = false) String serviceIdentifier,
            @RequestParam(value = "conversationId", required = false) String conversationId) {
        return pop(serviceIdentifier, conversationId);
    }

    @GetMapping("/pop")
    @Transactional
    public ResponseEntity pop(
            @RequestParam(value = "serviceIdentifier", required = false) String serviceIdentifier,
            @RequestParam(value = "conversationId", required = false) String conversationId) {

        NextMoveInMessage message;
        if (Strings.isNullOrEmpty(conversationId)) {
            NextMoveInMessageQueryInput query = new NextMoveInMessageQueryInput().setServiceIdentifier("DPE");
            Predicate p = inRepo.createQuery(query).and(QNextMoveInMessage.nextMoveInMessage.lockTimeout.isNotNull()).getValue();
            message = inRepo.findAll(p, PageRequests.FIRST_BY_LAST_UPDATED_ASC)
                    .getContent().stream().findFirst().orElseThrow(NoContentException::new);
        } else {
            message = inRepo.findByMessageId(conversationId)
                    .orElseThrow(() -> new MessageNotFoundException(conversationId));
        }

        if (message.getLockTimeout() == null) {
            throw new MessageNotLockedException(conversationId);
        }

        try {
            Resource asic = cryptoMessagePersister.read(conversationId, ASIC_FILE);
            cryptoMessagePersister.delete(conversationId);
            inRepo.delete(message);
            conversationService.registerStatus(conversationId, ReceiptStatus.INNKOMMENDE_LEVERT);

            if (message.getServiceIdentifier() == DPE) {
                StandardBusinessDocument statusSbd = sbdFactory.createStatusFrom(message.getSbd(), ReceiptStatus.LEVERT);
                if (statusSbd != null) {
                    NextMoveOutMessage msg = NextMoveOutMessage.of(statusSbd, DPE);
                    internalQueue.enqueueNextMove(msg);
                }
            }

            return ResponseEntity.ok()
                    .header(HEADER_CONTENT_DISPOSITION, HEADER_FILENAME + ASIC_FILE)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(asic);
        } catch (PersistenceException | IOException e) {
            Audit.error(String.format("Can not read file \"%s\" for message [conversationId=%s, sender=%s]. Removing message from queue",
                    ASIC_FILE, message.getMessageId(), message.getSenderIdentifier()), markerFrom(message), e);
            throw new FileNotFoundException(ASIC_FILE);
        }
    }

    @GetMapping("/delete")
    public ResponseEntity delete(
            @RequestParam(value = "serviceIdentifier", required = false) String serviceIdentifier,
            @RequestParam(value = "conversationId", required = false) String conversationId) {
        return ResponseEntity.ok().build();
    }
}
