package no.difi.meldingsutveksling.nextmove.nhn;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DPHMessageStatus (String receiverHerId,
                                TransportStatus transportStatus,
                                @JsonProperty("apprecStatus")
                                ApprecStatus apprecStatus) {}

