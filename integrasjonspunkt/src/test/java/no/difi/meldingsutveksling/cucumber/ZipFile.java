package no.difi.meldingsutveksling.cucumber;

import lombok.Data;

@Data
class ZipFile {

    private String fileName;
    private byte[] bytes;
}
