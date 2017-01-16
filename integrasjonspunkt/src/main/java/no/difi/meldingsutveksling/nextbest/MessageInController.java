package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "senderId", required = false) String senderId) {

        if (!isNullOrEmpty(conversationId)) {
            Optional<IncomingConversationResource> resource = Optional.ofNullable(repo.findOne(conversationId));
            if (resource.isPresent()) {
                return ResponseEntity.ok(resource.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No conversation with supplied id found.");
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
            String fileName = resource.get().getFileRefs().get(0);
            String filedir = props.getNextbest().getFiledir();
            if (!filedir.endsWith("/")) {
                filedir = filedir+"/";
            }
            filedir = filedir+resource.get().getConversationId()+"/";
            File file = new File(filedir+fileName);

            InputStreamResource isr = new InputStreamResource(new FileInputStream(file));

            repo.delete(resource.get());

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachement; filename="+fileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(isr);
        }
        return ResponseEntity.notFound().build();
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

    @RequestMapping(value = "/in/messages/asic", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getZipFromMessage(
            @RequestParam(value = "conversationId") String conversationId) throws IOException {

        Optional<IncomingConversationResource> resource = Optional.ofNullable(repo.findOne(conversationId));
        if (!resource.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No conversation with supplied id found.");
        }

        String filedir = props.getNextbest().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir+"/";
        }
        filedir = filedir+conversationId+"/";
        File file = new File(filedir+"message.zip");

        InputStreamResource isr = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachement; filename=message.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(isr);
    }

    @RequestMapping(value = "/in/messages/file", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getFileFromMessage(
            @RequestParam(value = "conversationId") String conversationId,
            @RequestParam(value = "fileId") Integer fileId) throws IOException {

        Optional<IncomingConversationResource> resource = Optional.ofNullable(repo.findOne(conversationId));
        if (!resource.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No conversation with supplied id found.");
        }

        if (!resource.get().getFileRefs().containsKey(fileId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No file with given id found for conversation");
        }

        try {
            byte[] fileFromAsic = getFileFromAsic(resource.get(), fileId);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachement; filename="+resource.get().getFileRefs().get(fileId))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileFromAsic.length)
                    .body(fileFromAsic);
        } catch (MessageException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File with given id not found in archive.");
        }
    }

    public byte[] getFileFromAsic(IncomingConversationResource resource, Integer fileId) throws IOException,
            MessageException {

        String filedir = props.getNextbest().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir+"/";
        }
        filedir = filedir+resource.getConversationId()+"/";
        File file = new File(filedir+"message.zip");

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().equals(resource.getFileRefs().get(fileId))) {
                    return IOUtils.toByteArray(zipInputStream);
                }
            }
        }
        throw new MessageException(StatusMessage.UNABLE_TO_EXTRACT_ZIP_CONTENTS);
    }
}
