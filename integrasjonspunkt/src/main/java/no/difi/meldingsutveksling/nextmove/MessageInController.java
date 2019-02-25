package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import io.swagger.annotations.*;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;
import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

@RestController
@Api
public class MessageInController {

    private static final Logger log = LoggerFactory.getLogger(MessageInController.class);

    private static final String NO_CONVO_FOUND = "No conversation with supplied id found.";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String HEADER_FILENAME = "attachement; filename=";

    private IntegrasjonspunktProperties props;
    private ConversationService conversationService;
    private DirectionalConversationResourceRepository repo;
    private MessagePersister messagePersister;
    private IntegrasjonspunktNokkel keyInfo;

    public MessageInController(IntegrasjonspunktProperties props,
                               ConversationService conversationService,
                               ConversationResourceRepository cRepo,
                               ObjectProvider<MessagePersister> messagePersister,
                               IntegrasjonspunktNokkel keyInfo) {
        this.props = props;
        this.conversationService = conversationService;
        this.repo = new DirectionalConversationResourceRepository(cRepo, INCOMING);
        this.messagePersister = messagePersister.getIfUnique();
        this.keyInfo = keyInfo;
    }

    @RequestMapping(value = "/in/messages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all incoming messages")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResource[].class),
            @ApiResponse(code = 404, message = "Not found", response = String.class),
            @ApiResponse(code = 204, message = "No countent", response = String.class)
    })
    public ResponseEntity getIncomingMessages(
            @ApiParam(value = "Service Identifier")
            @RequestParam(value = "serviceIdentifier", required = false) ServiceIdentifier serviceIdentifier,
            @ApiParam(value = "Conversation id")
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @ApiParam(value = "Sender id")
            @RequestParam(value = "senderId", required = false) String senderId) {

        if (!isNullOrEmpty(conversationId)) {
            Optional<ConversationResource> resource = repo.findByConversationId(conversationId);
            if (resource.isPresent()) {
                return ResponseEntity.ok(resource.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder().error("not_found")
                    .errorDescription(NO_CONVO_FOUND).build());
        }

        List<ConversationResource> resources;
        if (serviceIdentifier != null) {
            if (!isNullOrEmpty(senderId)) {
                resources = repo.findByServiceIdentifierAndSenderId(serviceIdentifier, senderId);
            } else {
                resources = repo.findByServiceIdentifier(serviceIdentifier);
            }
        } else {
            if (!isNullOrEmpty(senderId)) {
                resources = repo.findBySenderId(senderId);
            } else {
                resources = Lists.newArrayList(repo.findAll());
            }
        }

        if (resources.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(resources);
    }

    @RequestMapping(value = "/in/messages/peek", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Peek and lock incoming queue", notes = "Gets the first message in the incoming queue, then locks the message")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResource.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    @Transactional
    public ResponseEntity peekLockIncomingMessages(
            @ApiParam(value = "Service Identifier")
            @RequestParam(value = "serviceIdentifier", required = false) ServiceIdentifier serviceIdentifier) {

        Optional<ConversationResource> resource;
        if (serviceIdentifier == null) {
            resource = repo.findFirstByLockTimeoutIsNullOrderByLastUpdateAsc();
        } else {
            resource = repo.findFirstByServiceIdentifierAndLockTimeoutIsNullOrderByLastUpdateAsc(serviceIdentifier);
        }

        if (resource.isPresent()) {
            ConversationResource cr = resource.get();
            cr.setLockTimeout(LocalDateTime.now().plusMinutes(props.getNextmove().getLockTimeoutMinutes()));
            repo.save(cr);
            log.info(markerFrom(cr), "Conversation with id={} locked", cr.getConversationId());
            return ResponseEntity.ok(cr);
        }
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/in/messages/peek", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Peek incoming queue", notes = "Gets the first message in the incoming queue")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResource.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    public ResponseEntity peekIncomingMessages(
            @ApiParam(value = "Service Identifier")
            @RequestParam(value = "serviceIdentifier", required = false) ServiceIdentifier serviceIdentifier) {

        Optional<ConversationResource> resource;
        if (serviceIdentifier == null) {
            resource = repo.findFirstByLockTimeoutIsNullOrderByLastUpdateAsc();
        } else {
            resource = repo.findFirstByServiceIdentifierAndLockTimeoutIsNullOrderByLastUpdateAsc(serviceIdentifier);
        }

        if (resource.isPresent()) {
            return ResponseEntity.ok(resource.get());
        }
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/in/messages/unlock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Unlock message", notes = "Unlock the first queued locked message")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = InputStreamResource.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    @Transactional
    public ResponseEntity unlockMessage(
            @RequestParam(value = "serviceIdentifier", required = false) Optional<ServiceIdentifier> serviceIdentifier,
            @RequestParam(value = "conversationId", required = false) Optional<String> conversationId) {

        Optional<ConversationResource> resource;
        if (conversationId.isPresent()) {
            resource = repo.findByConversationId(conversationId.get());
        }
        else if (serviceIdentifier.isPresent()) {
            resource = repo.findFirstByServiceIdentifierAndLockTimeoutIsNotNullOrderByLastUpdateAsc(serviceIdentifier.get());
        } else {
            resource = repo.findFirstByLockTimeoutIsNotNullOrderByLastUpdateAsc();
        }

        if (resource.isPresent()) {
            ConversationResource cr = resource.get();
            cr.setLockTimeout(null);
            repo.save(cr);
            log.info(markerFrom(cr), "Conversation with id={} unlocked", cr.getConversationId());
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/in/messages/delete", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Remove message", notes = "Delete the first queued locked message")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = InputStreamResource.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    @Transactional
    public ResponseEntity deleteMessage(
            @RequestParam(value = "serviceIdentifier", required = false) Optional<ServiceIdentifier> serviceIdentifier,
            @RequestParam(value = "conversationId", required = false) Optional<String> conversationId) {

        Optional<ConversationResource> resource;
        if (conversationId.isPresent()) {
            resource = repo.findByConversationId(conversationId.get());
        }
        else if (serviceIdentifier.isPresent()) {
            resource = repo.findFirstByServiceIdentifierAndLockTimeoutIsNotNullOrderByLastUpdateAsc(serviceIdentifier.get());
        } else {
            resource = repo.findFirstByLockTimeoutIsNotNullOrderByLastUpdateAsc();
        }

        if (resource.isPresent()) {
            ConversationResource cr = resource.get();
            if (cr.getLockTimeout() == null) {
                return ResponseEntity.badRequest().body(notLockedErrorResponse());
            }

            try {
                messagePersister.delete(cr);
            } catch (IOException e) {
                log.error("Error deleting files from conversation with id={}", cr.getConversationId(),  e);
            }

            repo.delete(cr);
            Optional<Conversation> c = conversationService.registerStatus(cr.getConversationId(),
                    MessageStatus.of(GenericReceiptStatus.INNKOMMENDE_LEVERT));
            c.ifPresent(conversationService::markFinished);
            Audit.info(format("Conversation with id=%s popped from queue", cr.getConversationId()),
                    markerFrom(cr));
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/in/messages/pop", method = RequestMethod.GET)
    @ApiOperation(value = "Pop incoming queue", notes = "Gets the ASiC for the first non locked message in the queue, " +
            "unless conversationId is specified, then removes it.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = InputStreamResource.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    @Transactional
    public ResponseEntity popMessage(
            @ApiParam(value = "Service Identifier")
            @RequestParam(value = "serviceIdentifier", required = false) Optional<ServiceIdentifier> serviceIdentifier,
            @RequestParam(value = "conversationId", required = false) Optional<String> conversationId) throws IOException {

        Optional<ConversationResource> resource;
        if (conversationId.isPresent()) {
            resource = repo.findByConversationId(conversationId.get());
        }
        else if (serviceIdentifier.isPresent()) {
            resource = repo.findFirstByServiceIdentifierAndLockTimeoutIsNullOrderByLastUpdateAsc(serviceIdentifier.get());
        } else {
            resource = repo.findFirstByLockTimeoutIsNullOrderByLastUpdateAsc();
        }

        if (resource.isPresent()) {
            ConversationResource cr = resource.get();
            String filename = cr.getFileRefs().get(0);

            FileEntryStream fileEntry;
            try {
                fileEntry = messagePersister.readStream(cr, ASIC_FILE);
            } catch (PersistenceException e) {
                Audit.error(String.format("Can not read file \"%s\" for message [conversationId=%s, sender=%s]. Removing message from queue",
                        filename, cr.getConversationId(), cr.getSenderId()), markerFrom(cr), e);
                repo.delete(cr);
                return fileNotFoundErrorResponse(filename);
            }

            CmsUtil cmsUtil = new CmsUtil();
            InputStream decryptedAsic = cmsUtil.decryptCMSStreamed(fileEntry.getInputStream(), keyInfo.loadPrivateKey());
            fileEntry.getInputStream().close();

            return ResponseEntity.ok()
                    .header(HEADER_CONTENT_DISPOSITION, HEADER_FILENAME+ASIC_FILE)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileEntry.getSize())
                    .body(new InputStreamResource(decryptedAsic));
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity fileNotFoundErrorResponse(String filename) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .error("file_not_found")
                .errorDescription(format("File %s not found on server", filename))
                .build());
    }

    private ResponseEntity notLockedErrorResponse() {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .error("message not locked")
                .errorDescription("Message is not locked and can thus not be deleted")
                .build());
    }

}
