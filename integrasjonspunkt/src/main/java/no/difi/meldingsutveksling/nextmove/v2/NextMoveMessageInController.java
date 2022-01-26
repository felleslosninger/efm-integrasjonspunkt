package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.AsicUtils;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.AsicPersistenceException;
import no.difi.meldingsutveksling.exceptions.FileNotFoundException;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import org.slf4j.MDC;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@RestController
@Validated
@RequestMapping("/api/messages/in")
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageInController {

    private static final MediaType MIMETYPE_ASICE = MediaType.parseMediaType(AsicUtils.MIMETYPE_ASICE);
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String HEADER_FILENAME = "attachment; filename=";

    private final NextMoveMessageInService messageService;

    @GetMapping
    @Transactional
    public Page<StandardBusinessDocument> findMessages(
            @Valid NextMoveInMessageQueryInput input,
            @PageableDefault Pageable pageable) {
        return messageService.findMessages(input, pageable);
    }

    @GetMapping(value = "peek")
    public StandardBusinessDocument peek(@Valid NextMoveInMessageQueryInput input) {
        NextMoveInMessage message = messageService.peek(input)
                .orElseThrow(NoContentException::new);
        MDC.put(NextMoveConsts.CORRELATION_ID, message.getMessageId());
        Audit.info(String.format("Message [id=%s] locked until %s", message.getMessageId(), message.getLockTimeout()), markerFrom(message));
        return message.getSbd();
    }

    @GetMapping(value = "pop/{messageId}")
    public ResponseEntity<InputStreamResource> popMessage(@PathVariable("messageId") String messageId) {
        MDC.put(NextMoveConsts.CORRELATION_ID, messageId);
        try {
            InputStreamResource asic = messageService.popMessage(messageId);
            if (asic == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok()
                    .header(HEADER_CONTENT_DISPOSITION, HEADER_FILENAME + ASIC_FILE)
                    .contentType(MIMETYPE_ASICE)
                    .body(asic);
        } catch (AsicPersistenceException e) {
            throw new FileNotFoundException(ASIC_FILE);
        }
    }

    @DeleteMapping(value = "/{messageId}")
    @Transactional
    public StandardBusinessDocument deleteMessage(@PathVariable("messageId") String messageId) {
        MDC.put(NextMoveConsts.CORRELATION_ID, messageId);
        return messageService.deleteMessage(messageId);
    }
}
