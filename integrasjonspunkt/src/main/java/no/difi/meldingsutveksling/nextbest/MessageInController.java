package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import io.swagger.annotations.*;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.nextbest.ConversationDirection.INCOMING;
import static no.difi.meldingsutveksling.nextbest.logging.ConversationResourceMarkers.markerFrom;

@RestController
@Api
public class MessageInController {

    private static final Logger log = LoggerFactory.getLogger(MessageInController.class);

    private static final String NO_CONVO_FOUND = "No conversation with supplied id found.";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String HEADER_FILENAME = "attachement; filename=";

    private DirectionalConversationResourceRepository repo;

    @Autowired
    private IntegrasjonspunktProperties props;

    @Autowired
    public MessageInController(ConversationResourceRepository cRepo) {
        repo = new DirectionalConversationResourceRepository(cRepo, INCOMING);
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
            resource = repo.findFirstByOrderByLastUpdateAsc();
        } else {
            resource = repo.findFirstByServiceIdentifierOrderByLastUpdateAsc(serviceIdentifier);
        }

        if (resource.isPresent()) {
            return ResponseEntity.ok(resource.get());
        }
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/in/messages/pop", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Pop incoming queue", notes = "Gets the ASiC for the first message in queue, then removes " +
            "the message from the queue")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = InputStreamResource.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    public ResponseEntity popIncomingMessages(
            @ApiParam(value = "Service Identifier")
            @RequestParam(value = "serviceIdentifier", required = false) Optional<ServiceIdentifier> serviceIdentifier,
            @RequestParam(value = "conversationId", required = false) Optional<String> conversationId)
            throws FileNotFoundException {

        Optional<ConversationResource> resource;
        if (conversationId.isPresent()) {
            resource = repo.findByConversationId(conversationId.get());
        }
        else if (serviceIdentifier.isPresent()) {
            resource = repo.findFirstByServiceIdentifierOrderByLastUpdateAsc(serviceIdentifier.get());
        } else {
            resource = repo.findFirstByOrderByLastUpdateAsc();
        }

        if (resource.isPresent()) {
            String filedir = props.getNextbest().getFiledir();
            if (!filedir.endsWith("/")) {
                filedir = filedir+"/";
            }
            filedir = filedir+resource.get().getConversationId()+"/";
            String filename = resource.get().getFileRefs().get(0);
            File file = new File(filedir+filename);

            InputStreamResource isr = new InputStreamResource(new FileInputStream(file));

            repo.delete(resource.get());
            log.info(markerFrom(resource.get()), "Conversation with id={} popped from queue", resource.get().getConversationId());

            return ResponseEntity.ok()
                    .header(HEADER_CONTENT_DISPOSITION, HEADER_FILENAME+filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(isr);
        }
        return ResponseEntity.noContent().build();
    }

}
