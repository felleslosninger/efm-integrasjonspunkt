package no.difi.meldingsutveksling.status.service;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.jpa.ObjectMapperHolder;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.nhn.ApplicationReceiptError;
import no.difi.meldingsutveksling.nextmove.nhn.FeilmeldingForApplikasjonskvittering;
import no.difi.meldingsutveksling.nextmove.nhn.IncomingReceipt;
import no.difi.meldingsutveksling.nextmove.nhn.NhnAdapterClient;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.receipt.StatusQueue;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusQueryInput;
import no.difi.meldingsutveksling.status.MessageStatusRepository;
import no.difi.meldingsutveksling.view.Views;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/statuses")
public class MessageStatusController {

    private static String FASTLEGE_PROCESS = "urn:no:difi:profile:digitalpost:fastlege:ver1.0";
    private static String NHN_PROCESS = "urn:no:difi:profile:digitalpost:helse:ver1.0";

    private final MessageStatusRepository statusRepo;
    private final StatusQueue statusQueue;
    private final NhnAdapterClient nhnAdapterClient;
    private final ObjectMapperHolder objectMapperHolder;

    @GetMapping
    @JsonView(Views.MessageStatus.class)
    @Transactional(readOnly = true)
    public Page<MessageStatus> find(
            @Valid MessageStatusQueryInput input,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDateTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDateTime,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        var messageStatus = statusRepo.find(input, pageable);

        messageStatus.get().forEach(t-> {
            if(isDphMessage(t) && shouldRetrieveApprecInfo(t)) {
                decorateWithApprecInfo(t);
            }
        });

        return messageStatus;
    }



    @GetMapping("{messageId}")
    @JsonView(Views.MessageStatus.class)
    @Transactional(readOnly = true)
    public Page<MessageStatus> findByMessageId(
            @PathVariable String messageId,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        var status = statusRepo.findByConversationMessageId(messageId, pageable);
        status.get().forEach(t-> {
            if(isDphMessage(t) && shouldRetrieveApprecInfo(t)) {
                decorateWithApprecInfo(t);
            }
        });

        return status;
    }

    @GetMapping("peek")
    @JsonView(Views.MessageStatus.class)
    @Transactional
    public MessageStatus peekLatest() {
        Optional<Long> statusId = statusQueue.receiveStatus();
        return statusId.map(s -> statusRepo.findById(s)
                    .orElseThrow(() -> new NextMoveRuntimeException("MessageStatus with id=%s not found in DB".formatted(s))))
                .orElseThrow(NoContentException::new);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> removeStatus(@PathVariable Long id) {
        if (statusQueue.removeStatus(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private static boolean isDphMessage(MessageStatus t) {
        return Objects.equals(t.getConversation().getProcessIdentifier(), FASTLEGE_PROCESS) || Objects.equals(t.getConversation().getProcessIdentifier(), NHN_PROCESS);
    }

    private static boolean shouldRetrieveApprecInfo(MessageStatus t) {
        return Objects.equals(t.getStatus(), ReceiptStatus.FEIL.name());
    }

    private void decorateWithApprecInfo(MessageStatus t) {
        String rawReceipt = "";
        IncomingReceipt receiptIn = nhnAdapterClient.messageReceipt(UUID.fromString(t.getConversation().getMessageReference()), t.getConversation().getSender()).getLast();

        try {
            HashMap<String, Object> reciept = new HashMap<>();
            reciept.put("status", receiptIn.status());
            reciept.put("error", receiptIn.errors());
            rawReceipt = ObjectMapperHolder.get().writeValueAsString(reciept);
        } catch (Exception e) {
            try {
                rawReceipt= ObjectMapperHolder.get().writeValueAsString(Map.of("error",List.of(new ApplicationReceiptError(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL_FORMAT,"Unable to format apprec information"),
                    "status",receiptIn.status())));
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }
        t.setRawReceipt(rawReceipt);
    }
}
