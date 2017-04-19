package no.difi.meldingsutveksling.receipt;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MessageReceiptRepository extends CrudRepository<MessageReceipt, String> {
    List<MessageReceipt> findByGenIdGreaterThanEqual(Integer id);
}
