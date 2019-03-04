package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.domain.sbdh.Receiver;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import org.junit.Test;

import java.util.HashSet;

import static no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader.DocumentType.KVITTERING;
import static no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader.DocumentType.MELDING;
import static org.junit.Assert.assertEquals;

public class StandardBusinessDocumentHeaderTest {

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void testShouldFailOnWrongReceiverListsizeZero() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        header.getReceiver();
        header.getReceiverOrganisationNumber();
    }

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void testShouldFailOnWrongReceiverListsizeOneOrMore() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        header.getReceiver().add(new Receiver());
        header.getReceiver().add(new Receiver());
        header.getReceiverOrganisationNumber();
    }

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void testMissingIdentifierOnPartner() {
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        header.getReceiver().add(new Receiver());
        header.getReceiverOrganisationNumber();
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
                .from(Organisasjonsnummer.fromIso6523("123456789"))
                .to(Organisasjonsnummer.fromIso6523("123456789"))
                .relatedToJournalPostId("some journalpost")
                .relatedToConversationId("some conversation")
                .type(KVITTERING)
                .build();
        assertEquals(StandardBusinessDocumentHeader.KVITTERING_TYPE, h.getDocumentIdentification().getType());
        assertEquals(StandardBusinessDocumentHeader.KVITTERING_VERSION, h.getDocumentIdentification().getTypeVersion());
    }

    @Test
    public void testBuildMelding() {
        StandardBusinessDocumentHeader h = new StandardBusinessDocumentHeader.Builder()
                .from(Organisasjonsnummer.fromIso6523("123456789"))
                .to(Organisasjonsnummer.fromIso6523("123456789"))
                .relatedToJournalPostId("some journalpost")
                .relatedToConversationId("some conversation")
                .type(MELDING)
                .build();
        assertEquals(StandardBusinessDocumentHeader.MELDING_TYPE, h.getDocumentIdentification().getType());
        assertEquals(StandardBusinessDocumentHeader.MELDING_VERSION, h.getDocumentIdentification().getTypeVersion());
    }

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void testBuildWithoutType() {
        StandardBusinessDocumentHeader h = new StandardBusinessDocumentHeader.Builder()
                .from(Organisasjonsnummer.fromIso6523("123456789"))
                .to(Organisasjonsnummer.fromIso6523("123456789"))
                .relatedToJournalPostId("some journalpost")
                .relatedToConversationId("some conversation")
                .build();
    }

}