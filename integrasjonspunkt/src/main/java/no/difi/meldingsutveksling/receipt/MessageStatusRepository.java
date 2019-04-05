package no.difi.meldingsutveksling.receipt;

import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

public interface MessageStatusRepository extends PagingAndSortingRepository<MessageStatus, String>,
        QueryDslPredicateExecutor<MessageStatus>,
        QuerydslBinderCustomizer<QMessageStatus> {

    Optional<MessageStatus> findByStatId(Integer statId);

    List<MessageStatus> findAllByConvId(Integer convId);

    List<MessageStatus> findByStatIdGreaterThanEqual(Integer statId);

    List<MessageStatus> findAllByConvIdAndStatIdGreaterThanEqual(Integer convId, Integer recId);

    Optional<MessageStatus> findFirstByOrderByLastUpdateAsc();

    @Override
    default void customize(QuerydslBindings bindings, QMessageStatus root) {
        // NOOP
    }

    default Page<MessageStatus> find(MessageStatusQueryInput input, Pageable pageable) {
        return findAll(createQuery(input).getValue()
                , pageable);
    }

    default BooleanBuilder createQuery(MessageStatusQueryInput input) {
        BooleanBuilder builder = new BooleanBuilder();

        QMessageStatus messageStatus = QMessageStatus.messageStatus;

        if (input.getConvId() != null) {
            builder.and(messageStatus.convId.eq(input.getConvId()));
        }

        if (input.getConversationId() != null) {
            builder.and(messageStatus.conversationId.eq(input.getConversationId()));
        }

        if (input.getStatus() != null) {
            builder.and(messageStatus.status.eq(input.getStatus()));
        }

        return builder;
    }
}
