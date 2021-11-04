package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.dpi.client.domain.AsicEAttachable;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class CreateZip {

    public void zipIt(List<AsicEAttachable> files, OutputStream outputStream) {
        try (ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(outputStream)) {
            zipOutputStream.setEncoding(StandardCharsets.UTF_8.name());
            zipOutputStream.setMethod(ZipArchiveOutputStream.DEFLATED);
            for (AsicEAttachable file : files) {
                log.trace("Adding " + file.getFilename() + " to archive.");
                ZipArchiveEntry zipEntry = new ZipArchiveEntry(file.getFilename());
                zipOutputStream.putArchiveEntry(zipEntry);
                try (InputStream inputStream = file.getResource().getInputStream()) {
                    IOUtils.copy(inputStream, zipOutputStream);
                }
                zipOutputStream.closeArchiveEntry();
            }
            zipOutputStream.finish();
        } catch (IOException e) {
            throw new Exception("Failed to create XIP!", e);
        }
    }

    private static class Exception extends RuntimeException {
        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
