package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.AsicUtils;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.AsicPersistenceException;
import no.difi.meldingsutveksling.exceptions.AsicReadException;
import no.difi.meldingsutveksling.exceptions.FileNotFoundException;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import org.apache.commons.io.IOUtils;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import java.io.IOException;

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
    @Transactional
    public void popMessage(@PathVariable("messageId") String messageId, HttpServletResponse response) throws IOException {
        // Me tek i bruk underliggande Java Servlet API for å jobbe rundt problem knytt til transaksjonar i Spring og
        // strømming av BLOB fra Postgres:
        //
        // - Postgres krev open transaksjon for å strømme BLOB
        // - Spring avslutter transaksjonen i WebMVC-controlleren
        // - Dersom WebMVC-controlleren returnerer ein strøm blir denne behandla utanfor WebMVC-controlleren
        // - Derfor manglar transaksjonen på det tidspunktet strømmen blir behandla - dette resulterer i ein feil
        //
        // For å unngå dette bruker me underliggande Java Servlet API som lar oss gjere strømming i WebMVC-controlleren.

        MDC.put(NextMoveConsts.CORRELATION_ID, messageId);
        try {
            Resource asic = messageService.popMessage(messageId);
            if (asic == null) {
                response.setStatus(HttpStatus.NO_CONTENT.value());
                return;
            }
            if (!asic.isReadable()) {
                messageService.handleCorruptMessage(messageId);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                return;
            }
            response.setHeader(HEADER_CONTENT_DISPOSITION, HEADER_FILENAME + ASIC_FILE);
            response.setHeader("Content-Type", MIMETYPE_ASICE.toString());
            response.setStatus(HttpStatus.OK.value());
            IOUtils.copy(asic.getInputStream(), response.getOutputStream());
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
