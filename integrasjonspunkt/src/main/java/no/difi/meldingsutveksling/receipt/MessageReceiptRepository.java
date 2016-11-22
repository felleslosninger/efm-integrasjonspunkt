package no.difi.meldingsutveksling.receipt;

import org.springframework.data.repository.CrudRepository;

/**
 * Repository for message receipts.
 */
public interface MessageReceiptRepository extends CrudRepository<MessageReceipt, String> {
}
