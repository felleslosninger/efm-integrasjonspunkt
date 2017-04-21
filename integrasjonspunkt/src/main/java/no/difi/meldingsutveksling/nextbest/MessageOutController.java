package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.MessageContextException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.MessageStatus;
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
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.nextbest.logging.ConversationResourceMarkers.markerFrom;

@RestController
public class MessageOutController {

    private static final Logger log = LoggerFactory.getLogger(MessageOutController.class);

    @Autowired
    private OutgoingConversationResourceRepository repo;

    @Autowired
    private IncomingConversationResourceRepository incRepo;

    @Autowired
    private ConversationRepository convRepo;

    @Autowired
    private ServiceRegistryLookup sr;

    @Autowired
    private IntegrasjonspunktProperties props;

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private NextBestServiceBus nextBestServiceBus;


    private List<String> getSupportedTypes() {

        List<String> supportedTypes = Lists.newArrayList();
        if (props.getFeature().isEnableDPO()) {
            supportedTypes.add(ServiceIdentifier.DPO.fullname());
        }
        if (props.getFeature().isEnableDPE()) {
            supportedTypes.add(ServiceIdentifier.DPE_DATA.fullname());
            supportedTypes.add(ServiceIdentifier.DPE_INNSYN.fullname());
        }

        return supportedTypes;
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
            @RequestParam(value = "senderId", required = false) String senderId,
            @RequestParam(value = "conversationId", required = false) String conversationId) throws URISyntaxException {

        if (isNullOrEmpty(receiverId)) {
            return ResponseEntity.badRequest().body("Required String parameter \'receiverId\' is not present");
        }
        if (isNullOrEmpty(messagetypeId)) {
            return ResponseEntity.badRequest().body("Required String parameter \'messagetypeId\' is not present");
        }

        List<String> supportedTypes = getSupportedTypes();
        if (!supportedTypes.contains(messagetypeId)) {
            return ResponseEntity.badRequest().body("messagetypeId \'"+messagetypeId+"\' not supported. Supported " +
                    "types: "+supportedTypes);
        }

        String sender = isNullOrEmpty(senderId) ? props.getOrg().getNumber() : senderId;

        OutgoingConversationResource conversationResource;
        if (isNullOrEmpty(conversationId)) {
            conversationResource = OutgoingConversationResource.of(sender, receiverId, messagetypeId);
        } else {
            conversationResource = repo.findOne(conversationId);
            if (conversationResource == null) {
                conversationResource = OutgoingConversationResource.of(conversationId, sender, receiverId,
                        messagetypeId);
            }
        }

        repo.save(conversationResource);
        log.info(markerFrom(conversationResource), "Created new conversation resource with id={}",
                conversationResource.getConversationId());

        return ResponseEntity.ok(conversationResource);
    }

    @RequestMapping(value = "/out/messages/{conversationId}", method = RequestMethod.POST)
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
            log.info(markerFrom(conversationResource), "Adding file \"{}\" ({}, {} bytes) to {}",
                    file.getOriginalFilename(), file.getContentType(), file.getSize(), conversationResource.getConversationId());

            String filedir = props.getNextbest().getFiledir();
            if (!filedir.endsWith("/")) {
                filedir = filedir+"/";
            }
            filedir = filedir+conversationId+"/";
            File localFile = new File(filedir+file.getOriginalFilename());
            localFile.getParentFile().mkdirs();

            try {
                FileOutputStream os = new FileOutputStream(localFile);
                BufferedOutputStream bos = new BufferedOutputStream(os);
                bos.write(file.getBytes());
                bos.close();
                os.close();

                if (!conversationResource.getFileRefs().values().contains(file.getOriginalFilename())) {
                    conversationResource.addFileRef(file.getOriginalFilename());
                }
            } catch (java.io.IOException e) {
                log.error("Could not write file {f}", localFile, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not write file");
            }
        }

        try {
            if (ServiceIdentifier.DPE_INNSYN.fullname().equals(conversationResource.getMessagetypeId()) ||
                    ServiceIdentifier.DPE_DATA.fullname().equals(conversationResource.getMessagetypeId())) {
                if (!props.getNextbest().getServiceBus().isEnable()) {
                    String responseStr = String.format("Service Bus disabled, cannot send messages" +
                            " of types %s,%s", ServiceIdentifier.DPE_INNSYN.fullname(), ServiceIdentifier.DPE_DATA.fullname());
                    log.error(markerFrom(conversationResource), responseStr);
                    return ResponseEntity.badRequest().body(responseStr);
                }
                nextBestServiceBus.putMessage(conversationResource);
                log.info(markerFrom(conversationResource), "Message sent to service bus");
            } else {
                messageSender.sendMessage(conversationResource);
                log.info(markerFrom(conversationResource), "Message sent to altinn");
            }
        } catch (NextBestException | MessageContextException e) {
            log.error("Send message failed.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during sending. Check logs");
        }

        repo.delete(conversationResource);

        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/tracings/{conversationId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getTracings(
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "messagetypeId", required = false) String messagetypeId,
            @RequestParam(value = "lastonly", required = false) boolean lastonly) {
        List<Conversation> clist = convRepo.findByConversationId(conversationId);
        if (clist == null || clist.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (lastonly) {
            MessageStatus latestStatus = clist.get(0).getMessageStatuses().stream()
                    .sorted((a, b) -> b.getLastUpdate().compareTo(a.getLastUpdate()))
                    .findFirst().get();
            return ResponseEntity.ok(latestStatus);
        }
        return ResponseEntity.ok(clist.get(0));
    }

    @RequestMapping(value = "/out/types/{identifier}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getTypes(@PathVariable(value = "identifier", required = true) String identifier) {
        Optional<ServiceRecord> serviceRecord = Optional.ofNullable(sr.getServiceRecord(identifier));
        if (serviceRecord.isPresent()) {
            ArrayList<String> types = Lists.newArrayList();
            types.add(serviceRecord.get().getServiceIdentifier().toString());
            types.addAll(serviceRecord.get().getDpeCapabilities());
            return ResponseEntity.ok(types);
        }
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/out/types/{messagetypeId}/prototype", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getPrototype(
            @PathVariable("messagetypeId") String messagetypeId,
            @RequestParam(value = "receiverId", required = false) String receiverId) {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/transferqueue/{conversationId}", method = RequestMethod.GET)
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
