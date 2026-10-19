package no.difi.meldingsutveksling.ks.svarut;

import org.jspecify.annotations.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ForsendelseIdRepository extends CrudRepository<ForsendelseIdEntry, String> {

    @Transactional(readOnly = true)
    Optional<ForsendelseIdEntry> findByMessageId(String messageId);

    @Transactional
    void deleteByMessageId(String messageId);

    @Override
    @Transactional
    <S extends ForsendelseIdEntry> @NonNull S save(@NonNull S entity);
}
