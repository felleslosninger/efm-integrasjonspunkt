package no.difi.meldingsutveksling.domain;

public interface ByteArrayFile {
    String getFileName();
    byte[] getBytes();
    String getMimeType();
}
