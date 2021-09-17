package no.difi.meldingsutveksling.status;

import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Profile("!test")
public interface ConversationRepository extends PagingAndSortingRepository<Conversation, Long>,
        QuerydslPredicateExecutor<Conversation>,
        QuerydslBinderCustomizer<QConversation> {

    @EntityGraph(value = "Conversation.messageStatuses")
    default Page<Conversation> findWithMessageStatuses(ConversationQueryInput input, Pageable pageable) {
        Predicate p = createQuery(input).getValue();
        return p != null ? findAll(p, pageable) : findAll(pageable);
    }

    default Page<Conversation> findWithMessageStatuses(String input, String direction, LocalDate date, Pageable pageable) {
        Predicate p = freeSearchQuery(input, direction, date).getValue();
        return p != null ? findAll(p, pageable) : findAll(pageable);
    }

    @EntityGraph(value = "Conversation.messageStatuses")
    Optional<Conversation> findByIdAndDirection(Long id, ConversationDirection direction);

    @EntityGraph(value = "Conversation.messageStatuses")
    List<Conversation> findByMessageIdAndDirection(String messageId, ConversationDirection direction);

    @EntityGraph(value = "Conversation.messageStatuses")
    List<Conversation> findByConversationId(String conversationId);

    @EntityGraph(value = "Conversation.messageStatuses")
    Optional<Conversation> findByConversationIdAndDirection(String conversationId, ConversationDirection direction);

    @EntityGraph(value = "Conversation.messageStatuses")
    List<Conversation> findByMessageId(String messageId);

    @EntityGraph(value = "Conversation.messageStatuses")
    List<Conversation> findByPollable(boolean pollable);

    @EntityGraph(value = "Conversation.messageStatuses")
    Page<Conversation> findByPollable(boolean pollable, Pageable pageable);

    @Override
    @EntityGraph(value = "Conversation.messageStatuses")
    Iterable<Conversation> findAllById(Iterable<Long> longs);

    @EntityGraph(value = "Conversation.messageStatuses")
    List<Conversation> findByReceiverIdentifierAndDirection(String receiverIdentifier, ConversationDirection direction);

    @EntityGraph(value = "Conversation.messageStatuses")
    List<Conversation> findByDirection(ConversationDirection direction);

    void deleteByMessageId(String messageId);

    @Query("SELECT id FROM Conversation WHERE pollable = true")
    Page<Long> findIdsForPollableConversations(Pageable pageable);

    @Transactional(readOnly = true)
    @Query("SELECT id FROM Conversation WHERE expiry < :time AND finished = false")
    Iterable<Long> findIdsForExpiredConversations(@Param("time") OffsetDateTime time);

    Long countByPollable(boolean pollable);

    @Override
    default void customize(QuerydslBindings bindings, QConversation root) {
        bindings.bind(root.messageStatuses).first(
                (path, value) -> {
                    BooleanBuilder predicate = new BooleanBuilder();
                    value.forEach(o -> predicate.or(path.any().status.equalsIgnoreCase(o.getStatus())));
                    return predicate;
                });
    }

    default Page<Conversation> find(ConversationQueryInput input, Pageable pageable) {
        Predicate p = createQuery(input).getValue();
        return p != null ? findAll(p, pageable) : findAll(pageable);
    }

    default BooleanBuilder freeSearchQuery(String input, String direction, LocalDate date) {
        BooleanBuilder builder = new BooleanBuilder();
        QConversation conversation = QConversation.conversation;
        if (!Strings.isNullOrEmpty(direction)) {
            builder.and(conversation.direction.eq(ConversationDirection.valueOf(direction)));
        }
        if (date != null) {
            OffsetDateTime start = DateTimeUtil.toOffsetDateTime(date.atStartOfDay());
            OffsetDateTime end = DateTimeUtil.toOffsetDateTime(date.atTime(LocalTime.MAX));
            builder.and(conversation.lastUpdate.between(start, end));
        }
        if (Strings.isNullOrEmpty(input)) return builder;

        builder.andAnyOf(Arrays.stream(input.split("\\|\\|"))
                .map(s -> ExpressionUtils.allOf(Arrays.stream(s.trim().split("&&"))
                        .map(String::trim)
                        .map(sa -> {
                            Predicate p = ExpressionUtils.anyOf(conversation.conversationId.containsIgnoreCase(sa),
                                    conversation.messageId.containsIgnoreCase(sa),
                                    conversation.receiverIdentifier.eq(sa),
                                    conversation.messageReference.eq(sa),
                                    conversation.messageTitle.containsIgnoreCase(sa),
                                    conversation.messageStatuses.any().status.containsIgnoreCase(sa));
                            try {
                                return ExpressionUtils.anyOf(p, conversation.serviceIdentifier.eq(ServiceIdentifier.valueOf(sa.toUpperCase())));
                            } catch (IllegalArgumentException e) {
                            }
                            return p;
                        }).toArray(Predicate[]::new)))
                .toArray(Predicate[]::new)
        );
        return builder;
    }

    default BooleanBuilder createQuery(ConversationQueryInput input) {
        BooleanBuilder builder = new BooleanBuilder();

        QConversation conversation = QConversation.conversation;

        if (input.getConversationId() != null) {
            builder.and(conversation.conversationId.eq(input.getConversationId()));
        }

        if (input.getMessageId() != null) {
            builder.and(conversation.messageId.eq(input.getMessageId()));
        }

        if (input.getReceiver() != null) {
            builder.and(conversation.receiver.eq(input.getReceiver()));
        }

        if (input.getReceiverIdentifier() != null) {
            builder.and(conversation.receiverIdentifier.eq(input.getReceiverIdentifier()));
        }

        if (input.getSender() != null) {
            builder.and(conversation.sender.eq(input.getSender()));
        }

        if (input.getSenderIdentifier() != null) {
            builder.and(conversation.senderIdentifier.eq(input.getSenderIdentifier()));
        }

        if (input.getServiceIdentifier() != null) {
            builder.and(conversation.serviceIdentifier.eq(ServiceIdentifier.valueOf(input.getServiceIdentifier())));
        }

        if (input.getMessageReference() != null) {
            builder.and(conversation.messageReference.eq(input.getMessageReference()));
        }

        if (input.getMessageTitle() != null) {
            builder.and(conversation.messageTitle.eq(input.getMessageTitle()));
        }

        if (input.getPollable() != null) {
            builder.and(conversation.pollable.eq(input.getPollable()));
        }

        if (input.getFinished() != null) {
            builder.and(conversation.finished.eq(input.getFinished()));
        }

        if (input.getDirection() != null) {
            builder.and(conversation.direction.eq(input.getDirection()));
        }

        return builder;
    }
}
