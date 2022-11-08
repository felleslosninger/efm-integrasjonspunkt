package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.arkivverket.standarder.noark5.arkivmelding.Korrespondansepart;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.DuplicateFilenameException;
import no.difi.meldingsutveksling.exceptions.InvalidKorrespondanseparttypeException;
import no.difi.meldingsutveksling.exceptions.MultipartFileToLargeException;
import no.difi.meldingsutveksling.exceptions.TimeToLiveException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/api/messages/out")
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageOutController {

    private static final int MAX_SIZE = 5 * 1024 * 1024;

    private final NextMoveMessageService messageService;
    private final OnBehalfOfNormalizer onBehalfOfNormalizer;
    private final ArkivmeldingUtil arkivmeldingUtil;

    @PostMapping(value = "multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StandardBusinessDocument createAndSendMessage(@RequestParam("sbd") @NotNull @Valid StandardBusinessDocument sbd,
                                                         @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
                                                         MultipartRequest multipartRequest) throws IOException, JAXBException {
        MDC.put(NextMoveConsts.CORRELATION_ID, sbd.getMessageId());
        MDC.put(HttpHeaders.USER_AGENT, userAgent);
        onBehalfOfNormalizer.normalize(sbd);
        List<MultipartFile> files = multipartRequest.getMultiFileMap().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Check for max size
        files.stream()
                .filter(p -> p.getSize() > MAX_SIZE)
                .findAny()
                .ifPresent(p -> {
                    throw new MultipartFileToLargeException(p.getOriginalFilename(), MAX_SIZE);
                });
        // Check for duplicate filenames
        List<String> filenames = files.stream()
                .map(MultipartFile::getOriginalFilename)
                .collect(Collectors.toList());
        filenames.stream()
                .filter(f -> Collections.frequency(filenames, f) > 1)
                .reduce((a, b) -> a + ", " + b)
                .ifPresent(d -> {
                    throw new DuplicateFilenameException(d);
                });

        for (MultipartFile file : files) {
            if (file.getName().equals("arkivmelding.xml")) {
                String xml = new String(file.getBytes());
                StringReader sr = new StringReader(xml);
                JAXBContext jaxbContext = JAXBContext.newInstance(Arkivmelding.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                Arkivmelding response = (Arkivmelding) unmarshaller.unmarshal(sr);
                Journalpost journalpost = arkivmeldingUtil.getJournalpost(response);

                for (Korrespondansepart part : journalpost.getKorrespondansepart()) {
                    if (part.getKorrespondanseparttype() == null) {
                        throw new InvalidKorrespondanseparttypeException();
                    }
                }
            }
        }

        NextMoveOutMessage message = messageService.createMessage(sbd, files);
        messageService.sendMessage(message.getId());
        return message.getSbd();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(noRollbackFor = TimeToLiveException.class)
    public StandardBusinessDocument createMessage(@Valid @RequestBody StandardBusinessDocument sbd,
                                                  @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        MDC.put(NextMoveConsts.CORRELATION_ID, sbd.getMessageId());
        MDC.put(HttpHeaders.USER_AGENT, userAgent);
        onBehalfOfNormalizer.normalize(sbd);
        NextMoveOutMessage message = messageService.createMessage(sbd);
        return message.getSbd();
    }

    @GetMapping
    @Transactional
    public Page<StandardBusinessDocument> getMessages(@QuerydslPredicate(root = NextMoveOutMessage.class) Predicate predicate,
                                                      @PageableDefault Pageable pageable) {
        return messageService.findMessages(predicate, pageable)
                .map(NextMoveOutMessage::getSbd);
    }

    @GetMapping("/{messageId}")
    @Transactional
    public StandardBusinessDocument getMessage(@PathVariable("messageId") String messageId) {
        MDC.put(NextMoveConsts.CORRELATION_ID, messageId);
        return messageService.getMessage(messageId).getSbd();
    }

    @DeleteMapping("/{messageId}")
    @Transactional
    public void deleteMessage(@PathVariable("messageId") String messageId) {
        MDC.put(NextMoveConsts.CORRELATION_ID, messageId);
        messageService.deleteMessage(messageId);
    }

    @PutMapping(value = "/{messageId}")
    public void uploadFile(@PathVariable("messageId") String messageId,
                           @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                           @RequestHeader(HttpHeaders.CONTENT_DISPOSITION) String contentDisposition,
                           @RequestParam(value = "title", required = false) String title,
                           HttpServletRequest request) {
        MDC.put(NextMoveConsts.CORRELATION_ID, messageId);
        NextMoveOutMessage message = messageService.getMessage(messageId);
        messageService.addFile(message, new NextMoveUploadedFile(contentType, contentDisposition, title, request));
    }

    @PostMapping("/{messageId}")
    public void sendMessage(@PathVariable("messageId") String messageId) {
        MDC.put(NextMoveConsts.CORRELATION_ID, messageId);
        NextMoveOutMessage message = messageService.getMessage(messageId);
        messageService.sendMessage(message);
    }

}
