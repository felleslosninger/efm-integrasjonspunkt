package no.difi.meldingsutveksling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Standard {

    LEGACY("urn:no:difi:meldingsutveksling:1.0");

    private final String value;
}
