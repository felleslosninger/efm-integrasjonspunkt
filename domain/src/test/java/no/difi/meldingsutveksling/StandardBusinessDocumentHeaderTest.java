package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.domain.sbdh.Receiver;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class StandardBusinessDocumentHeaderTest {

    @Test
    public void testShouldFailOnWrongReceiverListsizeZero() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        header.getReceiver();
        assertThrows(MeldingsUtvekslingRuntimeException.class, () -> header.getReceiverOrganisationNumber());
    }

    @Test
    public void testShouldFailOnWrongReceiverListsizeOneOrMore() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        header.getReceiver().add(new Receiver().setIdentifier(new PartnerIdentification()));
        header.getReceiver().add(new Receiver().setIdentifier(new PartnerIdentification()));
        assertThrows(MeldingsUtvekslingRuntimeException.class, () -> header.getReceiverOrganisationNumber());
    }

    @Test
    public void testMissingIdentifierOnPartner() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        header.getReceiver().add(new Receiver());
        assertThrows(MeldingsUtvekslingRuntimeException.class, () -> header.getReceiverOrganisationNumber());
    }

    @Test
    public void positiveTest() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        Receiver p = new Receiver();
        final PartnerIdentification value = new PartnerIdentification();
        value.setAuthority("authorotai");
        value.setValue("011076111111");
        p.setIdentifier(value);
        header.getReceiver().add(p);
        header.getReceiverOrganisationNumber();
    }

    @Test
    public void testBuildKvittering() {
        StandardBusinessDocumentHeader h = new StandardBusinessDocumentHeader.Builder()
                .from(Organisasjonsnummer.from("123456789"))
                .to(Organisasjonsnummer.from("123456789"))
                .relatedToJournalPostId("some journalpost")
                .relatedToConversationId("some conversation")
                .relatedToMessageId("some messageId")
                .process("some process")
                .documentType("some document type")
                .type("some type")
                .build();
        assertEquals(h.getDocumentIdentification().getStandard(), "some document type");
        assertEquals(h.getDocumentIdentification().getType(), "some type");
        assertEquals(h.getDocumentIdentification().getTypeVersion(), "2.0");
    }

    @Test
    public void testBuildWithoutType() {
        StandardBusinessDocumentHeader.Builder builder = new StandardBusinessDocumentHeader.Builder()
            .from(Organisasjonsnummer.from("123456789"))
            .to(Organisasjonsnummer.from("123456789"))
            .relatedToJournalPostId("some journalpost")
            .relatedToConversationId("some conversation")
            .relatedToMessageId("some messageId");
        assertThrows(MeldingsUtvekslingRuntimeException.class, () -> builder.build());
    }

}