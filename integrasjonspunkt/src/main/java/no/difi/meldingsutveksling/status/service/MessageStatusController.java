package no.difi.meldingsutveksling.status.service;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
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
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/statuses")
public class MessageStatusController {

    private final MessageStatusRepository statusRepo;
    private final StatusQueue statusQueue;

    @GetMapping
    @JsonView(Views.MessageStatus.class)
    @Transactional(readOnly = true)
    public Page<MessageStatus> find(
            @Valid MessageStatusQueryInput input,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDateTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDateTime,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
            return statusRepo.find(input, pageable);
    }

    @GetMapping("{messageId}")
    @JsonView(Views.MessageStatus.class)
    @Transactional(readOnly = true)
    public Page<MessageStatus> findByMessageId(
            @PathVariable String messageId,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return statusRepo.findByConversationMessageId(messageId, pageable);
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
}
