package no.difi.meldingsutveksling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Process {

    LEGACY("urn:no:difi:meldingsutveksling:1.0"),
    ARKIVMELDING("urn:no:difi:profile:arkivmelding:ver1.0"),
    ARKIVMELDING_ADMINISTRASJON("urn:no:difi:profile:arkivmelding:administrasjon:ver1.0"),
    EINNSYN("urn:no:difi:profile:einnsyn:ver1.0");

    private final String value;
}
