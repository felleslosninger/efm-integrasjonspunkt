package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import no.difi.meldingsutveksling.nextmove.QNextMoveInMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NextMoveMessageInRepository extends PagingAndSortingRepository<NextMoveInMessage, Long>,
        CrudRepository<NextMoveInMessage, Long>,
        QuerydslPredicateExecutor<NextMoveInMessage>,
        QuerydslBinderCustomizer<QNextMoveInMessage>,
        PeekNextMoveMessageIn {

    @Override
    default void customize(QuerydslBindings bindings, QNextMoveInMessage root) {
        bindings.excluding(root.sbd);
    }

    List<NextMoveInMessage> findByLockTimeoutLessThanEqual(OffsetDateTime now);

    Optional<NextMoveInMessage> findByMessageId(String messageId);

    @Transactional(readOnly = true)
    @Query("SELECT id FROM NextMoveInMessage WHERE messageId = ?1")
    Optional<Long> findIdByMessageId(String messageId);

    @Transactional
    @Modifying
    @Query("DELETE FROM NextMoveInMessage WHERE id = ?1")
    void deleteMessageById(Long messageId);

    default Page<StandardBusinessDocument> find(NextMoveInMessageQueryInput input, Pageable pageable) {
        Predicate p = createQuery(input).getValue();
        Page<NextMoveInMessage> page = p != null ? findAll(p, pageable) : findAll(pageable);
        return page.map(NextMoveInMessage::getSbd);
    }

    default BooleanBuilder createQuery(NextMoveInMessageQueryInput input) {
        BooleanBuilder builder = new BooleanBuilder();

        QNextMoveInMessage nextMoveInMessage = QNextMoveInMessage.nextMoveInMessage;

        if (input.getConversationId() != null) {
            builder.and(nextMoveInMessage.conversationId.eq(input.getConversationId()));
        }

        if (input.getMessageId() != null) {
            builder.and(nextMoveInMessage.messageId.eq(input.getMessageId()));
        }

        if (input.getReceiverIdentifier() != null) {
            builder.and(nextMoveInMessage.receiverIdentifier.eq(input.getReceiverIdentifier()));
        }

        if (input.getSenderIdentifier() != null) {
            builder.and(nextMoveInMessage.senderIdentifier.eq(input.getSenderIdentifier()));
        }

        if (input.getServiceIdentifier() != null) {
            builder.and(nextMoveInMessage.serviceIdentifier.eq(ServiceIdentifier.valueOf(input.getServiceIdentifier())));
        }

        if (input.getProcess() != null) {
            builder.and(nextMoveInMessage.processIdentifier.eq(input.getProcess()));
        }

        return builder;
    }
}
