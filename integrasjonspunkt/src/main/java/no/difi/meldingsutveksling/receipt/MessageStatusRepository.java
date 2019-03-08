package no.difi.meldingsutveksling.receipt;

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
}
