package no.difi.meldingsutveksling.receipt;

import com.querydsl.core.BooleanBuilder;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Profile("!test")
public interface ConversationRepository extends PagingAndSortingRepository<Conversation, String>,
        QueryDslPredicateExecutor<Conversation>,
        QuerydslBinderCustomizer<QConversation> {

    Optional<Conversation> findByConvIdAndDirection(Integer convId, ConversationDirection direction);

    List<Conversation> findByConversationIdAndDirection(String conversationId, ConversationDirection direction);

    List<Conversation> findByConversationId(String conversationId);

    List<Conversation> findByPollable(boolean pollable);

    Page<Conversation> findByPollable(boolean pollable, Pageable pageable);

    List<Conversation> findByReceiverIdentifierAndDirection(String receiverIdentifier, ConversationDirection direction);

    List<Conversation> findByDirection(ConversationDirection direction);

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
        return findAll(createQuery(input).getValue()
                , pageable);
    }

    default BooleanBuilder createQuery(ConversationQueryInput input) {
        BooleanBuilder builder = new BooleanBuilder();

        QConversation conversation = QConversation.conversation;

        if (input.getConversationId() != null) {
            builder.and(conversation.conversationId.eq(input.getConversationId()));
        }

        if (input.getReceiverIdentifier() != null) {
            builder.and(conversation.receiverIdentifier.eq(input.getReceiverIdentifier()));
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

        if (input.getMsh() != null) {
            builder.and(conversation.msh.eq(input.getMsh()));
        }

        if (input.getDirection() != null) {
            builder.and(conversation.direction.eq(input.getDirection()));
        }

        return builder;
    }
}
