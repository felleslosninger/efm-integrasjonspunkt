package no.difi.meldingsutveksling.nextbest;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface IncomingConversationResourceRepository extends CrudRepository<IncomingConversationResource, String> {

    List<IncomingConversationResource> findByMessagetypeId(String messagetypeId);
}
