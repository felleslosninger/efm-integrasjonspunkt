package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.*;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.xml.sax.SAXException;

import javax.transaction.Transactional;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;
import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

@RestController
@Api
public class MessageOutController {

    private static final Logger log = LoggerFactory.getLogger(MessageOutController.class);
    private static final String ARKIVMELDING_FILE = "arkivmelding.xml";

    private DirectionalConversationResourceRepository outRepo;
    private DirectionalConversationResourceRepository inRepo;

    @Autowired
    private ServiceRegistryLookup sr;

    @Autowired
    private IntegrasjonspunktProperties props;

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private NextMoveServiceBus nextMoveServiceBus;

    @Autowired
    private ConversationStrategyFactory strategyFactory;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private InternalQueue internalQueue;

    private MessagePersister messagePersister;

    @Autowired
    public MessageOutController(ConversationResourceRepository repo, MessagePersister messagePersister) {
        this.outRepo = new DirectionalConversationResourceRepository(repo, OUTGOING);
        this.inRepo = new DirectionalConversationResourceRepository(repo, INCOMING);
        this.messagePersister = messagePersister;
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
        List<ServiceRecord> serviceRecords = sr.getServiceRecords(cr.getReceiverId());
        List<ServiceIdentifier> acceptableServiceIdentifiers = serviceRecords.stream()
                .map(ServiceRecord::getServiceIdentifier)
                .collect(Collectors.toList());
        if (cr.getServiceIdentifier() == DPI &&
                !acceptableServiceIdentifiers.contains(DPI) &&
                acceptableServiceIdentifiers.contains(DPV)) {
            acceptableServiceIdentifiers.add(DPI);
        }
        if (!acceptableServiceIdentifiers.contains(cr.getServiceIdentifier())) {
            return ResponseEntity.badRequest().body(ErrorResponse.builder().error("serviceIdentifier_not_acceptable")
                    .errorDescription(String.format("ServiceIdentifier '%s' not acceptable by receiver. Acceptable types: %s",
                            cr.getServiceIdentifier(), acceptableServiceIdentifiers)).build());
        }

        setDefaults(cr);
        cr = outRepo.save(cr);
        conversationService.registerConversation(cr);
        log.info(markerFrom(cr), "Created new conversation resource [id={}, serviceIdentifier={}]",
                cr.getConversationId(), cr.getServiceIdentifier());

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
    @Transactional
    public ResponseEntity uploadFiles(
            @ApiParam(value = "Conversation id")
            @PathVariable("conversationId") String conversationId,
            MultipartHttpServletRequest request) {

        Optional<ConversationResource> find = outRepo.findByConversationId(conversationId);
        if (!find.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder().error("not_found")
                    .errorDescription("No conversation with supplied id found").build());
        }
        ConversationResource cr = find.get();

        Optional<String> arkivmeldingFile = request.getFileMap().values().stream()
                .map(MultipartFile::getOriginalFilename)
                .filter(ARKIVMELDING_FILE::equals)
                .findFirst();
        if (arkivmeldingFile.isPresent()) {
            ResponseEntity arkivmeldingResponse = handleArkivmelding(request, cr);
            if (arkivmeldingResponse.getStatusCode() != HttpStatus.OK) {
                return arkivmeldingResponse;
            }
        }

        ArrayList<String> files = Lists.newArrayList(request.getFileNames());
        // MOVE-414: temp fix until arkivmelding implementation
        if (files.contains("hoveddokument")) {
            Collections.swap(files, files.indexOf("hoveddokument"), 0);
        }
        for (String f : files) {
            MultipartFile file = request.getFile(f);
            log.trace(markerFrom(cr), "Adding file \"{}\" ({}, {} bytes) to {}",
                    file.getOriginalFilename(), file.getContentType(), file.getSize(), cr.getConversationId());

            try {
                if (cr.getCustomProperties().containsKey("base64") && "true".equalsIgnoreCase(cr.getCustomProperties().get("base64"))) {
                    messagePersister.write(cr, file.getOriginalFilename(),
                            Base64.getDecoder().decode(new String(file.getBytes()).getBytes(StandardCharsets.UTF_8)));
                } else {
                    messagePersister.write(cr, file.getOriginalFilename(), file.getBytes());
                }

                if (!cr.getFileRefs().values().contains(file.getOriginalFilename())) {
                    cr.addFileRef(file.getOriginalFilename());
                }
            } catch (java.io.IOException e) {
                log.error("Could not persist file {}", file.getOriginalFilename(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ErrorResponse.builder().error("persist_file_error").errorDescription("Could not persist file").build());
            }
        }

        List<ServiceRecord> serviceRecords = sr.getServiceRecords(cr.getReceiverId());
        boolean hasDpiRecord = serviceRecords.stream()
                .map(ServiceRecord::getServiceIdentifier)
                .anyMatch(si -> DPI == si);
        if (cr.getServiceIdentifier() == DPI && !hasDpiRecord) {
            cr = convertDpiToDpv(cr);
        }

        internalQueue.enqueueNextmove(cr);
        return ResponseEntity.ok().build();

    }

    private ConversationResource convertDpiToDpv(ConversationResource cr) {
        DpvConversationResource dpv = DpvConversationResource.of((DpiConversationResource) cr);
        // Update conversation to make it pollable for receipts
        conversationService.setServiceIdentifier(cr.getConversationId(), ServiceIdentifier.DPV);
        conversationService.setPollable(cr.getConversationId(), true);
        return dpv;
    }

    private ResponseEntity handleArkivmelding(MultipartHttpServletRequest request, ConversationResource cr) {
        MultipartFile file = request.getFileMap().values().stream()
                .filter(f -> ARKIVMELDING_FILE.equals(f.getOriginalFilename()))
                .findFirst().get();
        try {
            validateArkivmelding(file.getInputStream());
            cr.setArkivmelding(unmarshalArkivmelding(file.getInputStream()));
            cr.setHasArkivmelding(true);
        } catch (IOException e) {
            log.error("Could not read file {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder().error("read_file_error").errorDescription("Could not read file").build());
        } catch (SAXException e) {
            log.error("{} XML validation failed", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder().error("xml_validation_error").errorDescription("arkivmelding.xml validation error: "+e.getLocalizedMessage()).build());
        } catch (JAXBException e) {
            log.error("Could not unmarshal {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder().error("xml_unmarshal").errorDescription("arkivmelding.xml unmarshalling error: "+e.getLocalizedMessage()).build());
        }

        List<String> files = request.getFileMap().values().stream()
                .map(MultipartFile::getOriginalFilename)
                .collect(Collectors.toList());
        List<String> amFiles = ArkivmeldingUtil.getFilenames(cr.getArkivmelding());
        if (!files.containsAll(amFiles)) {
            String filesString = amFiles.stream().collect(Collectors.joining(", "));
            log.error("Arkivmelding: missing files from upload, expected [{}]", filesString);
            return ResponseEntity.badRequest().body(
                    ErrorResponse.builder().error("missing_files").errorDescription("missing files from upload: "+filesString).build());
        }

        return ResponseEntity.ok().build();
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
        List<ServiceRecord> serviceRecords = sr.getServiceRecords(identifier);
        if (!serviceRecords.isEmpty()) {
            return ResponseEntity.ok(serviceRecords.stream()
                    .map(ServiceRecord::getServiceIdentifier)
                    .map(ServiceIdentifier::toString)
                    .collect(Collectors.toList()));
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

    private void validateArkivmelding(InputStream is) throws SAXException {
        ClassPathResource md = new ClassPathResource("xsd/metadatakatalog.xsd");
        ClassPathResource am = new ClassPathResource("xsd/arkivmelding.xsd");
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = factory.newSchema(new Source[]{
                    new StreamSource(md.getInputStream()),
                    new StreamSource(am.getInputStream())
            });
            schema.newValidator().validate(new StreamSource(is));
        } catch (IOException e) {
            log.error("Error reading xsd", e);
        }
    }

    private Arkivmelding unmarshalArkivmelding(InputStream inputStream) throws JAXBException {
        JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Arkivmelding.class}, new HashMap());
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StreamSource(inputStream), Arkivmelding.class).getValue();
    }

}
