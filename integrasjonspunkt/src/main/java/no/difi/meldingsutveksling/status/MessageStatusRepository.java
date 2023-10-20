package no.difi.meldingsutveksling.status;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import java.util.Optional;

public interface MessageStatusRepository extends PagingAndSortingRepository<MessageStatus, Long>,
        QuerydslPredicateExecutor<MessageStatus>,
        QuerydslBinderCustomizer<QMessageStatus> {

    @EntityGraph("MessageStatus.conversation")
    Optional<MessageStatus> findById(Long id);

    @EntityGraph("MessageStatus.conversation")
    Page<MessageStatus> findByConversationMessageId(String messageId, Pageable pageable);

    @EntityGraph("MessageStatus.conversation")
    Optional<MessageStatus> findFirstByOrderByLastUpdateAsc();

    @Override
    default void customize(QuerydslBindings bindings, QMessageStatus root) {
        // NOOP
    }

    default Page<MessageStatus> find(MessageStatusQueryInput input, Pageable pageable) {
        Predicate p = createQuery(input).getValue();
        return p != null ? findAll(p, pageable) : findAll(pageable);
    }

    default BooleanBuilder createQuery(MessageStatusQueryInput input) {
        BooleanBuilder builder = new BooleanBuilder();

        QMessageStatus messageStatus = QMessageStatus.messageStatus;

        if (input.getId() != null) {
            builder.and(messageStatus.id.eq(input.getId()));
        }

        if (input.getConversationId() != null) {
            builder.and(messageStatus.conversation.conversationId.eq(input.getConversationId()));
        }

        if (input.getMessageId() != null) {
            builder.and(messageStatus.conversation.messageId.eq(input.getMessageId()));
        }

        if (input.getStatus() != null) {
            builder.and(messageStatus.status.eq(input.getStatus()));
        }

        if (input.getFromDateTime() != null && input.getToDateTime() != null) {
            BooleanExpression fromDateTimePredicate = messageStatus.lastUpdate.after(input.fromDateTime);
            BooleanExpression toDateTimePredicate = messageStatus.lastUpdate.before(input.toDateTime);
            builder.and(fromDateTimePredicate.and(toDateTimePredicate));
        }

        return builder;
    }
}
