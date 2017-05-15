package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.*;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.nextbest.ConversationDirection.INCOMING;
import static no.difi.meldingsutveksling.nextbest.ConversationDirection.OUTGOING;
import static no.difi.meldingsutveksling.nextbest.logging.ConversationResourceMarkers.markerFrom;

@RestController
@Api
public class MessageOutController {

    private static final Logger log = LoggerFactory.getLogger(MessageOutController.class);

    private DirectionalConversationResourceRepository outRepo;
    private DirectionalConversationResourceRepository inRepo;

    @Autowired
    private ServiceRegistryLookup sr;

    @Autowired
    private IntegrasjonspunktProperties props;

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private NextBestServiceBus nextBestServiceBus;

    @Autowired
    private ConversationStrategyFactory strategyFactory;

    @Autowired
    public MessageOutController(ConversationResourceRepository repo) {
        outRepo = new DirectionalConversationResourceRepository(repo, OUTGOING);
        inRepo = new DirectionalConversationResourceRepository(repo, INCOMING);
    }

    @RequestMapping(value = "/out/messages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all outgoing messages")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResource[].class)
    })
    public ResponseEntity getAllResources(
            @ApiParam(value = "Receiver id")
            @RequestParam(value = "receiverId", required = false) String receiverId,
            @ApiParam(value = "Service Identifier")
            @RequestParam(value = "serviceIdentifier", required = false) ServiceIdentifier serviceIdentifier) {

        List<ConversationResource> resources;
        if (!isNullOrEmpty(receiverId) && serviceIdentifier != null) {
            resources = outRepo.findByReceiverIdAndServiceIdentifier(receiverId, serviceIdentifier);
        }
        else if (!isNullOrEmpty(receiverId)) {
            resources = outRepo.findByReceiverId(receiverId);
        }
        else if (serviceIdentifier != null) {
            resources = outRepo.findByServiceIdentifier(serviceIdentifier);
        }
        else {
            resources = Lists.newArrayList(outRepo.findAll());
        }
        return ResponseEntity.ok(resources);
    }

    @RequestMapping(value = "/out/messages/{conversationId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Find message", notes = "Find message with given conversation id")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResource.class)
    })
    public ResponseEntity getStatusForMessage(
            @ApiParam(value = "Conversation id", required = true)
            @PathVariable("conversationId") String conversationId) {
        Optional<ConversationResource> resource = outRepo.findByConversationId(conversationId);
        if (resource.isPresent()) {
            return ResponseEntity.ok().body(resource.get());
        }
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/out/messages", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create conversation", notes = "Create a new conversation with the given values")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResource.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    public ResponseEntity createResource(
            @RequestBody ConversationResource cr) {

        if (isNullOrEmpty(cr.getReceiverId())) {
            return ResponseEntity.badRequest().body(ErrorResponse.builder().error("receiverId_not_present")
                    .errorDescription("Required String parameter \'receiverId\' is not present").build());
        }
        if (cr.getServiceIdentifier() == null) {
            return ResponseEntity.badRequest().body(ErrorResponse.builder().error("serviceIdentifier_not_present")
                    .errorDescription("Required String parameter \'serviceIdentifier\' is not present").build());
        }

        if (!strategyFactory.getEnabledServices().contains(cr.getServiceIdentifier())) {
            return ResponseEntity.badRequest().body(ErrorResponse.builder().error("serviceIdentifier_not_supported")
                    .errorDescription(String.format("serviceIdentifier '%s' not supported. Supported types: %s",
                            cr.getServiceIdentifier(), strategyFactory.getEnabledServices())).build());
        }

        ServiceRecord receiverServiceRecord = sr.getServiceRecord(cr.getReceiverId());
        if (receiverServiceRecord.getServiceIdentifier() == DPV &&
                cr.getServiceIdentifier() != DPV) {
            return ResponseEntity.badRequest().body(ErrorResponse.builder().error("not_in_elma")
                    .errorDescription("Receiver not found in ELMA, not creating message.").build());
        }

        setDefaults(cr);
        outRepo.save(cr);
        log.info(markerFrom(cr), "Created new conversation resource with id={}", cr.getConversationId());

        return ResponseEntity.ok(cr);
    }

    private void setDefaults(ConversationResource cr) {
        cr.setSenderId(isNullOrEmpty(cr.getSenderId()) ? props.getOrg().getNumber() : cr.getSenderId());
        InfoRecord senderInfo = sr.getInfoRecord(cr.getSenderId());
        InfoRecord receiverInfo = sr.getInfoRecord(cr.getReceiverId());
        cr.setSenderName(senderInfo.getOrganizationName());
        cr.setReceiverName(receiverInfo.getOrganizationName());
        cr.setLastUpdate(LocalDateTime.now());
        cr.setConversationId(isNullOrEmpty(cr.getConversationId()) ? UUID.randomUUID().toString() : cr.getConversationId());
        cr.setFileRefs(cr.getFileRefs() == null ? Maps.newHashMap() : cr.getFileRefs());
    }

    @RequestMapping(value = "/out/messages/{conversationId}", method = RequestMethod.POST)
    @ApiOperation(value = "Upload files and send", notes = "Upload files to a conversation and send")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = String.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class),
            @ApiResponse(code = 404, message = "Not found", response = String.class),
            @ApiResponse(code = 500, message = "Internal error", response = String.class)
    })
    public ResponseEntity uploadFiles(
            @ApiParam(value = "Conversation id")
            @PathVariable("conversationId") String conversationId,
            MultipartHttpServletRequest request) {

        Optional<ConversationResource> find = outRepo.findByConversationId(conversationId);
        if (!find.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder().error("not_found")
                    .errorDescription("No conversation with supplied id found").build());
        }
        ConversationResource conversationResource = find.get();

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
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ErrorResponse.builder().error("write_file_error").errorDescription("Could not write file").build());
            }
        }

        Optional<ConversationStrategy> strategy = strategyFactory.getStrategy(conversationResource);
        if (!strategy.isPresent()) {
            String errorStr = String.format("Cannot send message - serviceIdentifier \"%s\" not supported",
                    conversationResource.getServiceIdentifier());
            log.error(markerFrom(conversationResource), errorStr);
            return ResponseEntity.badRequest().body(ErrorResponse.builder().error("serviceIdentifier_not_supported")
                    .errorDescription(errorStr).build());
        }
        ResponseEntity response = strategy.get().send(conversationResource);
        if (response.getStatusCode() == HttpStatus.OK) {
            outRepo.delete(conversationResource);
        }
        return response;

    }

    @RequestMapping(value = "/out/types/{identifier}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Supported message types", notes = "Get a list of supported message types for this endpoint")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = String[].class),
            @ApiResponse(code = 404, message = "Not found", response = String.class)
    })
    public ResponseEntity getTypes(
            @ApiParam(value = "Identifier", required = true)
            @PathVariable(value = "identifier") String identifier) {
        Optional<ServiceRecord> serviceRecord = Optional.ofNullable(sr.getServiceRecord(identifier));
        if (serviceRecord.isPresent()) {
            ArrayList<String> types = Lists.newArrayList();
            types.add(serviceRecord.get().getServiceIdentifier().toString());
            types.addAll(serviceRecord.get().getDpeCapabilities());
            return ResponseEntity.ok(types);
        }
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/out/types/{serviceIdentifier}/prototype", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Prototypes", hidden = true)
    public ResponseEntity getPrototype(
            @PathVariable("serviceIdentifier") ServiceIdentifier serviceIdentifier,
            @RequestParam(value = "receiverId", required = false) String receiverId) {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/transferqueue/{conversationId}", method = RequestMethod.GET)
    @ApiOperation(value = "Transfer conversation between queue (internal use)", hidden = true)
    @ResponseBody
    public ResponseEntity transferQueue(@PathVariable("conversationId") String conversationId) {
        Optional<ConversationResource> resource = outRepo.findByConversationId(conversationId);
        if (resource.isPresent()) {
            resource.get().setDirection(INCOMING);
            inRepo.save(resource.get());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder().error("not_found")
                .errorDescription("Conversation with supplied id not found.").build());
    }

}
