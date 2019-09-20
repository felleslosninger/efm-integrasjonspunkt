package no.difi.meldingsutveksling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Process {

    LEGACY("urn:no:difi:meldingsutveksling:1.0"),
    ARKIVMELDING_RESPONSE("urn:no:difi:profile:arkivmelding:response:ver1.0"),
    EINNSYN_RESPONSE("urn:no:difi:profile:einnsyn:response:ver1.0");

    private final String value;
}
