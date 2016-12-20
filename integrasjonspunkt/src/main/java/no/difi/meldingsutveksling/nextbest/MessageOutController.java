package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

@RestController
public class MessageOutController {

    private static final Logger log = LoggerFactory.getLogger(MessageOutController.class);

    private static final String uploadDirPrefix = "upload/";

    @Autowired
    private OutgoingConversationResourceRepository repo;

    @Autowired
    private IncomingConversationResourceRepository incRepo;

    @Autowired
    private ServiceRegistryLookup sr;

    @RequestMapping(value = "receivers/{receiverId}/capabilities", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getCapabilities(@PathVariable("receiverId") String receiverId) {
        Optional<ServiceRecord> serviceRecord = Optional.ofNullable(sr.getServiceRecord(receiverId));
        if (serviceRecord.isPresent()) {
            return ResponseEntity.ok(Arrays.asList(serviceRecord.get().getServiceIdentifier()));
        }
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/out/messages", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getAllResources(
            @RequestParam(value = "receiverId", required = false) String receiverId,
            @RequestParam(value = "messagetypeId", required = false) String messagetypeId) {

        List<OutgoingConversationResource> resources;
        if (!isNullOrEmpty(receiverId) && !isNullOrEmpty(messagetypeId)) {
            resources = repo.findByReceiverIdAndMessagetypeId(receiverId, messagetypeId);
        }
        else if (!isNullOrEmpty(receiverId)) {
            resources = repo.findByReceiverId(receiverId);
        }
        else if (!isNullOrEmpty(messagetypeId)) {
            resources = repo.findByMessagetypeId(messagetypeId);
        }
        else {
            resources = Lists.newArrayList(repo.findAll());
        }
        return ResponseEntity.ok(resources);
    }

    @RequestMapping(value = "/out/messages/{conversationId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getStatusForMessage(@PathVariable("conversationId") String conversationId) {
        Optional<OutgoingConversationResource> resource = Optional.ofNullable(repo.findOne(conversationId));
        if (resource.isPresent()) {
            return ResponseEntity.ok().body(resource.get());
        }
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/out/messages", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity createResource(
            @RequestParam("receiverId") String receiverId,
            @RequestParam("messagetypeId") String messagetypeId,
            @RequestParam(value = "conversationId", required = false) String conversationId) throws URISyntaxException {

        if (isNullOrEmpty(receiverId)) {
            return ResponseEntity.badRequest().body("Required String parameter \'receiverId\' is not present");
        }
        if (isNullOrEmpty(messagetypeId)) {
            return ResponseEntity.badRequest().body("Required String parameter \'messagetypeId\' is not present");
        }

        OutgoingConversationResource conversationResource;
        if (isNullOrEmpty(conversationId)) {
            conversationResource = OutgoingConversationResource.of(receiverId, messagetypeId);
        } else {
            conversationResource = repo.findOne(conversationId);
            if (conversationResource == null) {
                conversationResource = OutgoingConversationResource.of(conversationId, receiverId, messagetypeId);
            }
        }
        repo.save(conversationResource);
        return ResponseEntity.ok(conversationResource);
    }

    @RequestMapping(value = "/out/messages/{conversationId}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity uploadFiles(
            @PathVariable("conversationId") String conversationId,
            MultipartHttpServletRequest request) {

        OutgoingConversationResource conversationResource = repo.findOne(conversationId);
        if (conversationResource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No conversation with supplied id found");
        }

        ArrayList<String> files = Lists.newArrayList(request.getFileNames());
        for (String f : files) {
            MultipartFile file = request.getFile(f);
            log.info("Key: "+file.getName());
            log.info("Name: "+file.getOriginalFilename());
            log.info("Content-Type: "+file.getContentType());
            log.info("Size (bytes): "+file.getSize());

            File localFile = new File(uploadDirPrefix+file.getOriginalFilename());
            localFile.getParentFile().mkdirs();

            try {
                FileOutputStream os = new FileOutputStream(localFile);
                BufferedOutputStream bos = new BufferedOutputStream(os);
                bos.write(file.getBytes());
                bos.close();
            } catch (java.io.IOException e) {
                log.error("Could not write file {f}", localFile, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not write file");
            }
        }

        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/tracings/{conversationId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getTracings(
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "messagetypeId", required = false) String messagetypeId,
            @RequestParam(value = "lastonly", required = false) boolean lastonly) {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/out/types", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getTypes(@PathVariable(value = "receiverId", required = false) String receiverId) {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/out/types/{messagetypeId}/prototype", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getPrototype(
            @PathVariable("messagetypeId") String messagetypeId,
            @RequestParam(value = "receiverId", required = false) String receiverId) {
        throw new UnsupportedOperationException();
    }

    @RequestMapping("/transferqueue/{conversationId}")
    @ResponseBody
    public ResponseEntity transferQueue(@PathVariable("conversationId") String conversationId) {
        Optional<OutgoingConversationResource> resource = Optional.ofNullable(repo.findOne(conversationId));
        if (resource.isPresent()) {
            repo.delete(resource.get());
            incRepo.save(IncomingConversationResource.of(resource.get()));
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Conversation with supplied id not found.");
    }

}
