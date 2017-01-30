package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
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

@RestController
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
    @ResponseBody
    public ResponseEntity getIncomingMessages(
            @RequestParam(value = "messagetypeId", required = false) String messagetypeId,
            @RequestParam(value = "conversationId", required = false) String conversationId,
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
        return ResponseEntity.ok(resources);
    }

    @RequestMapping(value = "/in/messages/peek", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity peekIncomingMessages(
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
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/in/messages/pop", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity popIncomingMessages(
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

            return ResponseEntity.ok()
                    .header(HEADER_CONTENT_DISPOSITION, HEADER_FILENAME+filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(isr);
        }
        return ResponseEntity.notFound().build();
    }

}
