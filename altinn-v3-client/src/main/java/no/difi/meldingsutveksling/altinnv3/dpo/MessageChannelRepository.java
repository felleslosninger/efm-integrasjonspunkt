package no.difi.meldingsutveksling.altinnv3.dpo;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface MessageChannelRepository extends CrudRepository<MessageChannelEntry, String> {

    Optional<MessageChannelEntry> findByMessageId(String messageId);
    void deleteByMessageId(String messageId);

}
