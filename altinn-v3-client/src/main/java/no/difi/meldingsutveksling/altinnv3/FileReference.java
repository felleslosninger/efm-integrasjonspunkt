package no.difi.meldingsutveksling.altinnv3;

import lombok.Value;

@Value
public class FileReference {
    private final String value;
    private final Integer receiptID;
}
