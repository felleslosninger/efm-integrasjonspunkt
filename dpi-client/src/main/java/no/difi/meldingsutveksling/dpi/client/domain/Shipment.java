package no.difi.meldingsutveksling.dpi.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import no.difi.meldingsutveksling.dokumentpakking.domain.Parcel;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.BusinessMessage;

import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Data
public class Shipment {

    private Iso6523 sender;
    private Iso6523 receiver;
    private String messageId;
    private String conversationId;
    private String channel;
    private OffsetDateTime expectedResponseDateTime;
    private BusinessMessage businessMessage;
    private Parcel parcel;
    private X509Certificate receiverBusinessCertificate;
    private String language = "NO";

    @JsonIgnore
    public <T> Optional<T> getBusinessMessage(Class<T> clazz) {
        return clazz.isInstance(businessMessage) ? Optional.of(clazz.cast(businessMessage)) : Optional.empty();
    }
}
