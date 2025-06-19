package no.difi.meldingsutveksling.altinnv3.DPO;

import lombok.Value;

@Value
public class FileReference {
    private final String value;
    private final Integer receiptID;
}
