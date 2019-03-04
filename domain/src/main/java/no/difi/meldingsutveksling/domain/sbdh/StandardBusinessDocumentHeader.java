//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.11.25 at 12:23:12 PM CET 
//


package no.difi.meldingsutveksling.domain.sbdh;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import org.hibernate.annotations.DiscriminatorOptions;

import javax.persistence.*;
import javax.xml.bind.annotation.*;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


/**
 * <p>Java class for StandardBusinessDocumentHeader complex type.
 * <p>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;complexType name="StandardBusinessDocumentHeader">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="HeaderVersion" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Sender" type="{http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader}Partner" maxOccurs="unbounded"/>
 *         &lt;element name="Receiver" type="{http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader}Partner" maxOccurs="unbounded"/>
 *         &lt;element name="DocumentIdentification" type="{http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader}DocumentIdentification"/>
 *         &lt;element name="Manifest" type="{http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader}Manifest" minOccurs="0"/>
 *         &lt;element name="BusinessScope" type="{http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader}BusinessScope" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StandardBusinessDocumentHeader", propOrder = {
        "headerVersion",
        "sender",
        "receiver",
        "documentIdentification",
        "manifest",
        "businessScope"
})
@Data
@Entity
@Table(name = "header")
public class StandardBusinessDocumentHeader {

    public enum DocumentType {KVITTERING, MELDING}

    public static final String STANDARD_IDENTIFIER = "urn:no:difi:meldingsutveksling:1.0";
    public static final String KVITTERING_TYPE = "kvittering";
    public static final String KVITTERING_VERSION = "urn:no:difi:meldingsutveksling:1.0";
    public static final String MELDING_TYPE = "melding";
    public static final String MELDING_VERSION = "urn:no:difi:meldingsutveksling:1.0";
    public static final String NEXTMOVE_TYPE = "nextmove";

    @Id
    @GeneratedValue
    @JsonIgnore
    @XmlTransient
    private Long id;

    @XmlElement(name = "HeaderVersion", required = true)
    protected String headerVersion;
    @XmlElement(name = "Sender", required = true)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "header_id", nullable = false)
    protected Set<Sender> sender;
    @XmlElement(name = "Receiver", required = true)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "header_id", nullable = false)
    protected Set<Receiver> receiver;
    @XmlElement(name = "DocumentIdentification", required = true)
    @Embedded
    protected DocumentIdentification documentIdentification;
    @XmlElement(name = "Manifest")
    @Embedded
    protected Manifest manifest;
    @XmlElement(name = "BusinessScope")
    @Embedded
    protected BusinessScope businessScope;

    public void setSender(Set<Sender> sender) {
        this.sender = sender;
    }

    public Set<Sender> getSender() {
        if (sender == null) {
            sender = new HashSet<>();
        }
        return this.sender;
    }

    public StandardBusinessDocumentHeader addSender(Sender partner) {
        getSender().add(partner);
        return this;
    }

    public Set<Receiver> getReceiver() {
        if (receiver == null) {
            receiver = new HashSet<>();
        }
        return this.receiver;
    }

    public StandardBusinessDocumentHeader addReceiver(Receiver partner) {
        getReceiver().add(partner);
        return this;
    }

    @JsonIgnore
    public String getReceiverOrganisationNumber() {

        if (receiver.size() != 1) {
            throw new MeldingsUtvekslingRuntimeException(String.valueOf(receiver.size()));
        }
        Partner partner = receiver.iterator().next();
        PartnerIdentification identifier = partner.getIdentifier();
        if (identifier == null) {
            throw new MeldingsUtvekslingRuntimeException();
        }
        return identifier.getValue();
    }

    public static class Builder {

        private static final String HEADER_VERSION = "1.0";

        private static final String TYPE_JOURNALPOST_ID = "JOURNALPOST_ID";
        private static final String TYPE_CONVERSATIONID = "CONVERSATION_ID";

        private Organisasjonsnummer avsender;
        private Organisasjonsnummer mottaker;
        private String journalPostId;
        private String conversationId;
        private DocumentType documentType;

        public Builder from(Organisasjonsnummer avsender) {
            this.avsender = avsender;
            return this;
        }

        public Builder to(Organisasjonsnummer mottaker) {
            this.mottaker = mottaker;
            return this;
        }

        public Builder relatedToJournalPostId(String journalPostId) {
            this.journalPostId = journalPostId;
            return this;
        }

        public Builder type(DocumentType documentType) {
            this.documentType = documentType;
            return this;
        }

        public Builder relatedToConversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public StandardBusinessDocumentHeader build() {
            return new StandardBusinessDocumentHeader()
                    .setHeaderVersion(HEADER_VERSION)
                    .addSender(createSender(avsender))
                    .addReceiver(createReciever(mottaker))
                    .setBusinessScope(createBusinessScope(fromConversationId(conversationId), fromJournalPostId(journalPostId)))
                    .setDocumentIdentification(createDocumentIdentification(documentType));
        }

        private Sender createSender(Organisasjonsnummer orgNummer) {
            Sender sender = new Sender();
            sender.setIdentifier(new PartnerIdentification()
                    .setValue(orgNummer.asIso6523())
                    .setAuthority(orgNummer.asIso6523()));
            return sender;
        }

        private Receiver createReciever(Organisasjonsnummer orgNummer) {
            Receiver sender = new Receiver();
            sender.setIdentifier(new PartnerIdentification()
                    .setValue(orgNummer.asIso6523())
                    .setAuthority(orgNummer.asIso6523()));
            return sender;
        }

        private DocumentIdentification createDocumentIdentification(DocumentType documentType) {
            if (documentType == null) {
                throw new MeldingsUtvekslingRuntimeException("DocumentType must be set");
            }

            switch (documentType) {
                case KVITTERING:
                    return createDocumentIdentification(KVITTERING_TYPE, KVITTERING_VERSION);
                case MELDING:
                    return createDocumentIdentification(MELDING_TYPE, MELDING_VERSION);
                default:
                    throw new MeldingsUtvekslingRuntimeException(String.format("Unsupported DocumentType: %s", documentType.name()));
            }
        }

        private DocumentIdentification createDocumentIdentification(String type, String version) {
            return new DocumentIdentification()
                    .setCreationDateAndTime(ZonedDateTime.now())
                    .setStandard(STANDARD_IDENTIFIER)
                    .setType(type)
                    .setTypeVersion(version)
                    .setInstanceIdentifier(UUID.randomUUID().toString());
        }

        private BusinessScope createBusinessScope(Scope... scopes) {
            return new BusinessScope()
                    .setScope(Arrays.asList(scopes));
        }

        private Scope fromJournalPostId(String journalPostId) {
            return createDefaultScope()
                    .setType(TYPE_JOURNALPOST_ID)
                    .setInstanceIdentifier(journalPostId);
        }

        private Scope fromConversationId(String conversationId) {
            return createDefaultScope()
                    .setType(TYPE_CONVERSATIONID)
                    .setInstanceIdentifier(conversationId);
        }

        private Scope createDefaultScope() {
            return new Scope().setIdentifier(STANDARD_IDENTIFIER);
        }
    }
}