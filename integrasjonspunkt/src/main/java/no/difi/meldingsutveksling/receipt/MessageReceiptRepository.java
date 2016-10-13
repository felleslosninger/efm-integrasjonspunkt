package no.difi.meldingsutveksling.receipt;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Repository for message receipts.
 */
public interface MessageReceiptRepository extends CrudRepository<MessageReceipt, String> {
    List<MessageReceipt> findByCompleted(boolean completed);
}
