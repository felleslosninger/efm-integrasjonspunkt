package no.difi.meldingsutveksling.nextmove;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
public class MakeSureLegacyModeSequencesInH2Test {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void verify_that_just_hibernate_sequences_has_been_created_in_h2() {
        var query = entityManager.createNativeQuery("SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES").getResultList();
        assertEquals(1, query.size());
        assertEquals("HIBERNATE_SEQUENCE", query.getFirst());
    }


    /*
    For Spring Boot 3.x (Hibernate 6+) the auto mode for sequences will create one sequence pr table
    instead of a shared hibernate sequence.

    For H2 this would have resulted in 8 sequences, like shown below, this test is to make sure code has
    legacy mode for sequences set correctly as default property.

    The property to set legcay mode is :
    spring.jpa.properties.hibernate.id.db_structure_naming_strategy=legacy

    The shared sequences produced by older Hibernate would be a single :
    HIBERNATE_SEQUENCE

    The sequences produced by Hibernate 6 would be :
    FORSENDELSE_ID_ENTRY_SEQ
    MESSAGE_CHANNEL_ENTRY_SEQ
    NEXT_MOVE_MESSAGE_SEQ
    WEBHOOK_SUBSCRIPTION_SEQ
    NEXT_MOVE_MESSAGE_ENTRY_SEQ
    BUSINESS_MESSAGE_FILE_SEQ
    CONVERSATION_SEQ
    MESSAGE_STATUS_SEQ
    */

}
