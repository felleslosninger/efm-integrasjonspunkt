package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface BusinessMessageFileRepository extends JpaRepository<BusinessMessageFile, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM BusinessMessageFile b WHERE b.message.id = ?1")
    void deleteFilesByMessageId(Long messageId);

}
