package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DpvVarselTransportType {
    EPOST("Epost"),
    SMS("SMS"),
    EPOSTOGSMS("EpostOgSMS");

    @JsonValue
    private final String fullname;
}
