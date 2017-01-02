package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

@RestController
public class MessageInController {

    private static final Logger log = LoggerFactory.getLogger(MessageInController.class);

    @Autowired
    private IncomingConversationResourceRepository repo;

    @RequestMapping(value = "/in/messages/{conversationId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getMessageById(@PathVariable("conversationId") String conversationId) {
        Optional<IncomingConversationResource> resource = Optional.ofNullable(repo.findOne(conversationId));
        if (resource.isPresent()) {
            return ResponseEntity.ok(resource.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No conversation with supplied id found.");
    }

    @RequestMapping(value = "/in/messages", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getIncomingMessages(
            @RequestParam(value = "messagetypeId", required = false) String messagetypeId) {
        List<IncomingConversationResource> resources;
        if (!isNullOrEmpty(messagetypeId)) {
            resources = repo.findByMessagetypeId(messagetypeId);
        } else {
            resources = Lists.newArrayList(repo.findAll());
        }
        return ResponseEntity.ok(resources);
    }

    @RequestMapping(value = "/in/messages/pop/{messagetypeId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity popIncomingMessages(
            @PathVariable(value = "messagetypeId", required = false) String messagetypeId) {

        Optional<IncomingConversationResource> resource;
        if (isNullOrEmpty(messagetypeId)) {
            resource = repo.findFirstByOrderByLastUpdateAsc();
        } else {
            resource = repo.findFirstByMessagetypeIdOrderByLastUpdateAsc(messagetypeId);
        }

        if (resource.isPresent()) {
            repo.delete(resource.get());
            return ResponseEntity.ok(resource);
        }
        return ResponseEntity.notFound().build();
    }

}
