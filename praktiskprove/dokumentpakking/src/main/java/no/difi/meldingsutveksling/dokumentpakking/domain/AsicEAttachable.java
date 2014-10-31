package no.difi.meldingsutveksling.dokumentpakking.domain;

public interface AsicEAttachable {
    String getFileName();
    byte[] getBytes();
    String getMimeType();
}
