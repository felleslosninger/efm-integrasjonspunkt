package no.difi.meldingsutveksling.status;

import jakarta.persistence.EntityManager;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ResourceLock("nextmove-plain-datajpatest")
@DataJpaTest
class ConversationRepositoryIT {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        conversationRepository.deleteAll();
    }

    @Test
    void testUpdateServiceIdentifier() {
        Conversation conversation = new Conversation();
        conversation.setConversationId("conv-1");
        conversation.setMessageId("msg-1");
        conversation.setServiceIdentifier(ServiceIdentifier.DPO);
        conversation.setPollable(true);
        conversation = conversationRepository.save(conversation);
        Long id = conversation.getId();

        conversationRepository.updateServiceIdentifier(id, ServiceIdentifier.DPE);
        entityManager.clear();

        Optional<Conversation> updated = conversationRepository.findById(id);
        assertThat(updated).isPresent();
        assertThat(updated.get().getServiceIdentifier()).isEqualTo(ServiceIdentifier.DPE);
    }

    @Test
    void testFindIdsForPollableConversations() {
        Conversation conv1 = new Conversation();
        conv1.setConversationId("conv-1");
        conv1.setMessageId("msg-1");
        conv1.setPollable(true);
        conv1 = conversationRepository.save(conv1);

        Conversation conv2 = new Conversation();
        conv2.setConversationId("conv-2");
        conv2.setMessageId("msg-2");
        conv2.setPollable(false);
        conversationRepository.save(conv2);

        Conversation conv3 = new Conversation();
        conv3.setConversationId("conv-3");
        conv3.setMessageId("msg-3");
        conv3.setPollable(true);
        conv3 = conversationRepository.save(conv3);

        Page<Long> pollableIds = conversationRepository.findIdsForPollableConversations(PageRequest.of(0, 10));

        assertThat(pollableIds.getContent()).containsExactlyInAnyOrder(conv1.getId(), conv3.getId());
    }

    @Test
    void testFindIdsForExpiredConversations() {
        OffsetDateTime now = OffsetDateTime.now();

        Conversation expired = new Conversation();
        expired.setConversationId("conv-exp");
        expired.setMessageId("msg-exp");
        expired.setExpiry(now.minusHours(1));
        expired.setFinished(false);
        expired = conversationRepository.save(expired);

        Conversation notExpired = new Conversation();
        notExpired.setConversationId("conv-not-exp");
        notExpired.setMessageId("msg-not-exp");
        notExpired.setExpiry(now.plusHours(1));
        notExpired.setFinished(false);
        conversationRepository.save(notExpired);

        Conversation finishedExpired = new Conversation();
        finishedExpired.setConversationId("conv-fin-exp");
        finishedExpired.setMessageId("msg-fin-exp");
        finishedExpired.setExpiry(now.minusHours(1));
        finishedExpired.setFinished(true);
        conversationRepository.save(finishedExpired);

        Iterable<Long> expiredIds = conversationRepository.findIdsForExpiredConversations(now);

        assertThat(expiredIds).containsExactly(expired.getId());
    }

    @Test
    void testFindByMessageIdAndDirection() {
        Conversation conversation = new Conversation();
        conversation.setConversationId("conv-dir");
        conversation.setMessageId("msg-dir");
        conversation.setDirection(ConversationDirection.INCOMING);
        conversationRepository.save(conversation);

        List<Conversation> foundIncoming = conversationRepository.findByMessageIdAndDirection("msg-dir", ConversationDirection.INCOMING);
        List<Conversation> foundOutgoing = conversationRepository.findByMessageIdAndDirection("msg-dir", ConversationDirection.OUTGOING);

        assertThat(foundIncoming).hasSize(1);
        assertThat(foundOutgoing).isEmpty();
    }

    @Test
    void testCountByPollable() {
        Conversation conv1 = new Conversation();
        conv1.setConversationId("c-1");
        conv1.setMessageId("m-1");
        conv1.setPollable(true);
        conversationRepository.save(conv1);

        Conversation conv2 = new Conversation();
        conv2.setConversationId("c-2");
        conv2.setMessageId("m-2");
        conv2.setPollable(true);
        conversationRepository.save(conv2);

        Conversation conv3 = new Conversation();
        conv3.setConversationId("c-3");
        conv3.setMessageId("m-3");
        conv3.setPollable(false);
        conversationRepository.save(conv3);

        assertThat(conversationRepository.countByPollable(true)).isEqualTo(2L);
        assertThat(conversationRepository.countByPollable(false)).isEqualTo(1L);
    }

    @Test
    void testDeleteByMessageId() {
        Conversation conv = new Conversation();
        conv.setConversationId("c-del");
        conv.setMessageId("m-del");
        conversationRepository.save(conv);

        assertThat(conversationRepository.findByMessageId("m-del")).hasSize(1);

        conversationRepository.deleteByMessageId("m-del");

        assertThat(conversationRepository.findByMessageId("m-del")).isEmpty();
    }

    @Test
    void testFindWithMessageStatusesByQueryInput() {
        Conversation conv = new Conversation();
        conv.setConversationId("c-query");
        conv.setMessageId("m-query");
        conv.setSender("sender-org");
        conv.setReceiver("receiver-org");
        conv.setServiceIdentifier(ServiceIdentifier.DPO);
        conv.setDirection(ConversationDirection.OUTGOING);
        conv.setPollable(true);
        conversationRepository.save(conv);

        ConversationQueryInput input = new ConversationQueryInput();
        input.setConversationId("c-query");
        input.setServiceIdentifier("DPO");
        input.setDirection(ConversationDirection.OUTGOING);

        Page<Conversation> page = conversationRepository.findWithMessageStatuses(input, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getMessageId()).isEqualTo("m-query");

        ConversationQueryInput mismatchInput = new ConversationQueryInput();
        mismatchInput.setConversationId("c-query");
        mismatchInput.setServiceIdentifier("DPE");

        Page<Conversation> mismatchPage = conversationRepository.findWithMessageStatuses(mismatchInput, PageRequest.of(0, 10));
        assertThat(mismatchPage.getContent()).isEmpty();
    }

    @Test
    void testFindWithMessageStatusesFreeSearch() {
        Conversation conv = new Conversation();
        conv.setConversationId("c-free");
        conv.setMessageId("m-free-123");
        conv.setMessageTitle("Special Title");
        conv.setDirection(ConversationDirection.INCOMING);
        conv.setServiceIdentifier(ServiceIdentifier.DPO);
        conversationRepository.save(conv);

        Page<Conversation> page = conversationRepository.findWithMessageStatuses("Special", "INCOMING", null, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getMessageId()).isEqualTo("m-free-123");

        Page<Conversation> noMatchPage = conversationRepository.findWithMessageStatuses("NonExistent", "INCOMING", null, PageRequest.of(0, 10));
        assertThat(noMatchPage.getContent()).isEmpty();
    }
}
