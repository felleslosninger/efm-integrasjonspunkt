package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.QNextMoveOutMessage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface NextMoveMessageOutRepository extends PagingAndSortingRepository<NextMoveOutMessage, Long>,
        CrudRepository<NextMoveOutMessage, Long>,
        QuerydslPredicateExecutor<NextMoveOutMessage>,
        QuerydslBinderCustomizer<QNextMoveOutMessage> {

    Optional<NextMoveOutMessage> findByConversationId(String conversationId);

    Optional<NextMoveOutMessage> findByMessageId(String messageId);

    @Transactional(readOnly = true)
    @Query("SELECT id FROM NextMoveOutMessage WHERE messageId = ?1")
    Optional<Long> findIdByMessageId(String messageId);

    @Transactional
    @Modifying
    @Query("DELETE FROM NextMoveOutMessage WHERE id = ?1")
    void deleteMessageById(Long messageId);

    @Override
    default void customize(QuerydslBindings bindings, QNextMoveOutMessage root) {
        bindings.excluding(root.sbd);
    }

}
