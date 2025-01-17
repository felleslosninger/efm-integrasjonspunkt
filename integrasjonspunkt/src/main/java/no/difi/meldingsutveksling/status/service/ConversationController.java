package no.difi.meldingsutveksling.status.service;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.ConversationNotFoundException;
import no.difi.meldingsutveksling.exceptions.MessageNotFoundException;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ConversationQueryInput;
import no.difi.meldingsutveksling.status.ConversationRepository;
import no.difi.meldingsutveksling.view.Views;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationRepository conversationRepository;

    @GetMapping
    @JsonView(Views.Conversation.class)
    @Transactional
    public Page<Conversation> find(
            @Valid ConversationQueryInput input,
            @PageableDefault(sort = "lastUpdate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return conversationRepository.findWithMessageStatuses(input, pageable);
    }

    @GetMapping("{id}")
    @JsonView(Views.Conversation.class)
    @Transactional
    public Conversation getById(
            @PathVariable Long id) {
        return conversationRepository.findByIdAndDirection(id, OUTGOING)
                .orElseThrow(() -> new ConversationNotFoundException(id));
    }

    @DeleteMapping("{id}")
    @Transactional
    public void deleteById(@PathVariable Long id) {
        conversationRepository.deleteById(id);
    }

    @GetMapping("messageId/{messageId}")
    @JsonView(Views.Conversation.class)
    @Transactional
    public Conversation getByMessageId(
            @PathVariable String messageId) {
        return conversationRepository.findByMessageIdAndDirection(messageId, OUTGOING)
                .stream()
                .findFirst()
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    @DeleteMapping("messageId/{messageId}")
    @Transactional
    public void deleteByMessageId(@PathVariable String messageId) {
        conversationRepository.deleteByMessageId(messageId);
    }

    @GetMapping("queue")
    @JsonView(Views.Conversation.class)
    @Transactional
    public Page<Conversation> queuedConversations(@PageableDefault(sort = "lastUpdate", direction = Sort.Direction.DESC) Pageable pageable) {
        return conversationRepository.findByPollable(true, pageable);
    }
}
