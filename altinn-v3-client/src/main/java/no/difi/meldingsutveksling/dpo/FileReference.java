package no.difi.meldingsutveksling.dpo;

import lombok.Value;

@Value
public class FileReference {
    private final String value;
    private final Integer receiptID;
}
