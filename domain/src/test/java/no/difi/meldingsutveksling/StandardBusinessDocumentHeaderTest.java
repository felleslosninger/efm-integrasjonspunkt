package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.domain.sbdh.Receiver;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        header.getReceiver().add(new Receiver().setIdentifier(new PartnerIdentification()));
        header.getReceiver().add(new Receiver().setIdentifier(new PartnerIdentification()));
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
                .from(Organisasjonsnummer.from("123456789"))
                .to(Organisasjonsnummer.from("123456789"))
                .relatedToJournalPostId("some journalpost")
                .relatedToConversationId("some conversation")
                .relatedToMessageId("some messageId")
                .process("some process")
                .documentType("some document type")
                .type("some type")
                .build();
        assertThat(h.getDocumentIdentification().getStandard()).isEqualTo("some document type");
        assertThat(h.getDocumentIdentification().getType()).isEqualTo("some type");
        assertThat(h.getDocumentIdentification().getTypeVersion()).isEqualTo("2.0");
    }

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void testBuildWithoutType() {
        StandardBusinessDocumentHeader h = new StandardBusinessDocumentHeader.Builder()
                .from(Organisasjonsnummer.from("123456789"))
                .to(Organisasjonsnummer.from("123456789"))
                .relatedToJournalPostId("some journalpost")
                .relatedToConversationId("some conversation")
                .relatedToMessageId("some messageId")
                .build();
    }

}