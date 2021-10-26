package no.difi.meldingsutveksling.dpi.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.BusinessMessage;

import java.time.OffsetDateTime;
import java.util.Optional;

@Data
public class Shipment {

    private PartnerIdentification senderOrganizationIdentifier;
    private PartnerIdentification receiverOrganizationIdentifier;
    private String messageId;
    private String conversationId;
    private String channel;
    private OffsetDateTime expectedResponseDateTime;
    private BusinessMessage businessMessage;
    private Parcel parcel;
    private BusinessCertificate receiverBusinessCertificate;
    private String language = "NO";

    @JsonIgnore
    public <T> Optional<T> getBusinessMessage(Class<T> clazz) {
        return clazz.isInstance(businessMessage) ? Optional.of(clazz.cast(businessMessage)) : Optional.empty();
    }
}
