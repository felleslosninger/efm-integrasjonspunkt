package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import static no.difi.meldingsutveksling.dokumentpakking.service.ScopeFactory.fromConversationId;
import static no.difi.meldingsutveksling.dokumentpakking.service.ScopeFactory.fromJournalPostId;

public class CreateSBD {
    private static final String STANDARD = "urn:no:difi:meldingsutveksling:1.0";
    private static final String HEADER_VERSION = "1.0";
    private static final String TYPE_VERSION = "1.0";

    public StandardBusinessDocument createSBD(Organisasjonsnummer avsender, Organisasjonsnummer mottaker, Object payload, String conversationId, String type, String journalPostId) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(createHeader(avsender, mottaker, conversationId, type, journalPostId))
                .setAny(payload);
    }

    private StandardBusinessDocumentHeader createHeader(Organisasjonsnummer avsender, Organisasjonsnummer mottaker,
                                                        String conversationId, String documentType, String
                                                                journalPostId) {
        return new StandardBusinessDocumentHeader()
                .setHeaderVersion(HEADER_VERSION)
                .addSender(createSender(avsender))
                .addReceiver(createReceiver(mottaker))
                .setDocumentIdentification(createDocumentIdentification(documentType))
                .setBusinessScope(createBusinessScope(fromConversationId(conversationId), fromJournalPostId(journalPostId)));
    }

    private Receiver createReceiver(Organisasjonsnummer orgNummer) {
        Receiver sender = new Receiver();
        fillPartner(sender, orgNummer);
        return sender;
    }

    private Sender createSender(Organisasjonsnummer orgNummer) {
        Sender sender = new Sender();
        fillPartner(sender, orgNummer);
        return sender;
    }

    private void fillPartner(Partner partner, Organisasjonsnummer orgNummer) {
        partner.setIdentifier(new PartnerIdentification()
                .setValue(orgNummer.asIso6523())
                .setAuthority(orgNummer.asIso6523()));
    }

    private DocumentIdentification createDocumentIdentification(String type) {
        return new DocumentIdentification()
                .setCreationDateAndTime(ZonedDateTime.now())
                .setStandard(STANDARD)
                .setType(type)
                .setTypeVersion(TYPE_VERSION)
                .setInstanceIdentifier(UUID.randomUUID().toString());
    }

    private BusinessScope createBusinessScope(Scope... scopes) {
        return new BusinessScope().setScope(Arrays.asList(scopes));
    }
}
