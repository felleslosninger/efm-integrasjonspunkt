package no.difi.meldingsutveksling.receipt;

import com.querydsl.core.BooleanBuilder;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;

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
}
