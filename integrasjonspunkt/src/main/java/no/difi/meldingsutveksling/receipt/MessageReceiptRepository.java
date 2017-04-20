package no.difi.meldingsutveksling.receipt;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface MessageReceiptRepository extends CrudRepository<MessageReceipt, String> {
    Optional<MessageReceipt> findByRecId(Integer recId);
    List<MessageReceipt> findAllByConvId(Integer convId);
    List<MessageReceipt> findByRecIdGreaterThanEqual(Integer id);
    List<MessageReceipt> findAllByConvIdAndRecIdGreaterThanEqual(Integer convId, Integer recId);
    Optional<MessageReceipt> findFirstByOrderByLastUpdateAsc();
}
