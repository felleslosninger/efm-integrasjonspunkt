package no.difi.meldingsutveksling.ks.svarut;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ForsendelseIdRepository extends CrudRepository<ForsendelseIdEntry, String> {

    Optional<ForsendelseIdEntry> findByMessageId(String messageId);
    void deleteByMessageId(String messageId);
}
