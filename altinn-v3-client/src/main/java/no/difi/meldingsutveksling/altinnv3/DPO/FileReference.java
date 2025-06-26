package no.difi.meldingsutveksling.altinnv3.DPO;

import lombok.Value;

import java.util.UUID;

@Value
public class FileReference {
    private final UUID fileReferenceId;
//    private final String value;
    private final Integer receiptID;
}
