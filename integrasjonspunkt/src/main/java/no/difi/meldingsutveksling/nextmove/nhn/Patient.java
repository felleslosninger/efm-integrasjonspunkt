package no.difi.meldingsutveksling.nextmove.nhn;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Patient(
    @JsonProperty("fnr")
    String fnr,
    @JsonProperty("firstName")
    String firstName,
    @JsonProperty("middleName")
    String minddleName,
    @JsonProperty("lastName")
    String lastName,
    @JsonProperty("phoneNumber")
    String phoneNumber
) {}
