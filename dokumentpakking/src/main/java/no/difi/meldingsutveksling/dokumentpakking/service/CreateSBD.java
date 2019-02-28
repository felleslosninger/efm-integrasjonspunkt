package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import static no.difi.meldingsutveksling.dokumentpakking.service.ScopeFactory.fromConversationId;
import static no.difi.meldingsutveksling.dokumentpakking.service.ScopeFactory.fromJournalPostId;

public class CreateSBD {
	public static final String STANDARD = "urn:no:difi:meldingsutveksling:1.0";
	public static final String HEADER_VERSION = "1.0";
	public static final String TYPE_VERSION = "1.0";

    public StandardBusinessDocument createSBD(Organisasjonsnummer avsender, Organisasjonsnummer mottaker, Object payload, String conversationId, String type, String journalPostId) {
		StandardBusinessDocument doc = new StandardBusinessDocument();
		doc.setStandardBusinessDocumentHeader(createHeader(avsender, mottaker, conversationId, type, journalPostId));
		doc.setAny(payload);
		return doc;
	}

	private StandardBusinessDocumentHeader createHeader(Organisasjonsnummer avsender, Organisasjonsnummer mottaker,
														String conversationId, String documentType, String
																journalPostId) {
		StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
		header.setHeaderVersion(HEADER_VERSION);
		header.getSender().add(createPartner(avsender));
		header.getReceiver().add(createPartner(mottaker));
		header.setDocumentIdentification(createDocumentIdentification(documentType));
		header.setBusinessScope(createBusinessScope(fromConversationId(conversationId), fromJournalPostId(journalPostId)));
		return header;
	}

	private Partner createPartner(Organisasjonsnummer orgNummer) {
		Partner partner = new Partner();
		PartnerIdentification partnerIdentification = new PartnerIdentification();
		partnerIdentification.setValue(orgNummer.asIso6523());
		partnerIdentification.setAuthority(orgNummer.asIso6523());
		partner.setIdentifier(partnerIdentification);
		return partner;
	}

	private DocumentIdentification createDocumentIdentification(String type) {
		DocumentIdentification doc = new DocumentIdentification();

		GregorianCalendar gCal = new GregorianCalendar();
		gCal.setTime(new Date());
		XMLGregorianCalendar xmlDate;
		try {
			xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCal);
			doc.setCreationDateAndTime(xmlDate);
		} catch (DatatypeConfigurationException e) {
			throw new MeldingsUtvekslingRuntimeException(e);
		}

		doc.setStandard(STANDARD);
		doc.setType(type);
		doc.setTypeVersion(TYPE_VERSION);
		doc.setInstanceIdentifier(UUID.randomUUID().toString());

		return doc;
	}

	private BusinessScope createBusinessScope(Scope... scopes) {
		BusinessScope bScope = new BusinessScope();
        bScope.setScope(Arrays.asList(scopes));
		return bScope;
	}
}
