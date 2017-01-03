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

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

@RestController
public class MessageInController {

    private static final Logger log = LoggerFactory.getLogger(MessageInController.class);

    @Autowired
    private IncomingConversationResourceRepository repo;

    @Autowired
    private IntegrasjonspunktProperties props;

    @RequestMapping(value = "/in/messages", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getIncomingMessages(
            @RequestParam(value = "messagetypeId", required = false) String messagetypeId,
            @RequestParam(value = "conversationId", required = false) String conversationId) {

        if (!isNullOrEmpty(conversationId)) {
            Optional<IncomingConversationResource> resource = Optional.ofNullable(repo.findOne(conversationId));
            if (resource.isPresent()) {
                return ResponseEntity.ok(resource.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No conversation with supplied id found.");
        }

        List<IncomingConversationResource> resources;
        if (!isNullOrEmpty(messagetypeId)) {
            resources = repo.findByMessagetypeId(messagetypeId);
        } else {
            resources = Lists.newArrayList(repo.findAll());
        }
        return ResponseEntity.ok(resources);
    }

    @RequestMapping(value = "/in/messages/pop", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity popIncomingMessages(
            @RequestParam(value = "messagetypeId", required = false) String messagetypeId) {

        Optional<IncomingConversationResource> resource;
        if (isNullOrEmpty(messagetypeId)) {
            resource = repo.findFirstByOrderByLastUpdateAsc();
        } else {
            resource = repo.findFirstByMessagetypeIdOrderByLastUpdateAsc(messagetypeId);
        }

        if (resource.isPresent()) {
            repo.delete(resource.get());
            return ResponseEntity.ok(resource.get());
        }
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/in/messages/file", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getFileFromMessage(
            @RequestParam(value = "conversationId") String conversationId,
            @RequestParam(value = "fileId") Integer fileId,
            HttpServletResponse response) throws IOException {

        Optional<IncomingConversationResource> resource = Optional.ofNullable(repo.findOne(conversationId));
        if (!resource.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No conversation with supplied id found.");
        }

        if (!resource.get().getFileRefs().containsKey(fileId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No file with given id found for conversation");
        }

        String fileName = resource.get().getFileRefs().get(fileId);
        String filedir = props.getNextbest().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir+"/";
        }
        filedir = filedir+conversationId+"/";
        File file = new File(filedir+fileName);

        InputStreamResource isr = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachement; filename="+fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(isr);
    }
}
