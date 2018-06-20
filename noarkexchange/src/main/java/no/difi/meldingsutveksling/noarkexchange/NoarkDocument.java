package no.difi.meldingsutveksling.noarkexchange;

import lombok.Data;

@Data
public class NoarkDocument {

    private String filename;
    private byte[] content;
}
