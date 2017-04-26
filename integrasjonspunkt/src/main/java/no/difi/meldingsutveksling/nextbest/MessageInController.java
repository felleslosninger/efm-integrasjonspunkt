package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import io.swagger.annotations.*;
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
import static no.difi.meldingsutveksling.nextbest.logging.ConversationResourceMarkers.markerFrom;

@RestController
@Api
public class MessageInController {

    private static final Logger log = LoggerFactory.getLogger(MessageInController.class);

    private static final String NO_CONVO_FOUND = "No conversation with supplied id found.";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String HEADER_FILENAME = "attachement; filename=";

    @Autowired
    private IncomingConversationResourceRepository repo;

    @Autowired
    private IntegrasjonspunktProperties props;

    @RequestMapping(value = "/in/messages", method = RequestMethod.GET)
    @ApiOperation(value = "Get all incoming messages")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResource[].class),
            @ApiResponse(code = 404, message = "Not found", response = String.class),
            @ApiResponse(code = 204, message = "No countent", response = String.class)
    })
    public ResponseEntity getIncomingMessages(
            @ApiParam(value = "Messagetype id")
            @RequestParam(value = "messagetypeId", required = false) String messagetypeId,
            @ApiParam(value = "Conversation id")
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @ApiParam(value = "Sender id")
            @RequestParam(value = "senderId", required = false) String senderId) {

        if (!isNullOrEmpty(conversationId)) {
            Optional<IncomingConversationResource> resource = Optional.ofNullable(repo.findOne(conversationId));
            if (resource.isPresent()) {
                return ResponseEntity.ok(resource.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NO_CONVO_FOUND);
        }

        List<IncomingConversationResource> resources;
        if (!isNullOrEmpty(messagetypeId)) {
            if (!isNullOrEmpty(senderId)) {
                resources = repo.findByMessagetypeIdAndSenderId(messagetypeId, senderId);
            } else {
                resources = repo.findByMessagetypeId(messagetypeId);
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

    @RequestMapping(value = "/in/messages/peek", method = RequestMethod.GET)
    @ApiOperation(value = "Peek incoming queue", notes = "Gets the first message in the incoming queue")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResource.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    public ResponseEntity peekIncomingMessages(
            @ApiParam(value = "Messagetype id")
            @RequestParam(value = "messagetypeId", required = false) String messagetypeId) {

        Optional<IncomingConversationResource> resource;
        if (isNullOrEmpty(messagetypeId)) {
            resource = repo.findFirstByOrderByLastUpdateAsc();
        } else {
            resource = repo.findFirstByMessagetypeIdOrderByLastUpdateAsc(messagetypeId);
        }

        if (resource.isPresent()) {
            return ResponseEntity.ok(resource.get());
        }
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/in/messages/pop", method = RequestMethod.GET)
    @ApiOperation(value = "Pop incoming queue", notes = "Gets the ASiC for the first message in queue, then removes " +
            "the message from the queue")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = InputStreamResource.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    public ResponseEntity popIncomingMessages(
            @ApiParam(value = "Messagetype id")
            @RequestParam(value = "messagetypeId", required = false) String messagetypeId) throws FileNotFoundException {

        Optional<IncomingConversationResource> resource;
        if (isNullOrEmpty(messagetypeId)) {
            resource = repo.findFirstByOrderByLastUpdateAsc();
        } else {
            resource = repo.findFirstByMessagetypeIdOrderByLastUpdateAsc(messagetypeId);
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
