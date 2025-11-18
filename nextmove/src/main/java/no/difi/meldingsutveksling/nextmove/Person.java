package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Person(
    @JsonProperty("fnr")
    String fnr,
    @JsonProperty("firstName")
    String firstName,
    @JsonProperty("middleName")
    String middleName,
    @JsonProperty("lastName")
    String lastName,
    @JsonProperty("phoneNumber")
    String phoneNumber
) {}
