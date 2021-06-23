package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public enum EinnsynType {
    @JsonProperty("innsynskrav")
    INNSYNSKRAV,
    @JsonProperty("publisering")
    PUBLISERING
}
