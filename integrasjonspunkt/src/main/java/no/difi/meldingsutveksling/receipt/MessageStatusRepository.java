package no.difi.meldingsutveksling.receipt;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface MessageStatusRepository extends CrudRepository<MessageStatus, String>, QueryDslPredicateExecutor<MessageStatus> {
    Optional<MessageStatus> findByStatId(Integer statId);
    List<MessageStatus> findAllByConvId(Integer convId);
    List<MessageStatus> findByStatIdGreaterThanEqual(Integer statId);
    List<MessageStatus> findAllByConvIdAndStatIdGreaterThanEqual(Integer convId, Integer recId);
    Optional<MessageStatus> findFirstByOrderByLastUpdateAsc();
}
