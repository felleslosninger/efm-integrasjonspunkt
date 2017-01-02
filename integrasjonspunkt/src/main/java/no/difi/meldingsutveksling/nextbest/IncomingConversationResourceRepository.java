package no.difi.meldingsutveksling.nextbest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncomingConversationResourceRepository extends JpaRepository<IncomingConversationResource, String> {

    List<IncomingConversationResource> findByMessagetypeId(String messagetypeId);
    Optional<IncomingConversationResource> findFirstByOrderByLastUpdateAsc();
    Optional<IncomingConversationResource> findFirstByMessagetypeIdOrderByLastUpdateAsc(String messagetypeId);
}
