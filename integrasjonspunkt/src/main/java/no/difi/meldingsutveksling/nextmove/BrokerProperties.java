package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrokerProperties {

    @JsonProperty("MessageId")
    private String messageId;

    @JsonProperty("LockToken")
    private String lockToken;

    @JsonProperty("SequenceNumber")
    private String sequenceNumber;
}
