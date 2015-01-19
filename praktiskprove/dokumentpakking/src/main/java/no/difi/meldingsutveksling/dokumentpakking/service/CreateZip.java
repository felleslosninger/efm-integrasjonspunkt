package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.domain.ByteArrayFile;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class CreateZip {
	
    public Archive zipIt(List<ByteArrayFile> files) {
        ByteArrayOutputStream archive = null;
        ZipArchiveOutputStream zipOutputStream = null;
        try {
            archive = new ByteArrayOutputStream();
            zipOutputStream = new ZipArchiveOutputStream(archive);
            zipOutputStream.setEncoding(Charsets.UTF_8.name());
            zipOutputStream.setMethod(ZipArchiveOutputStream.DEFLATED);
            for (ByteArrayFile file : files) {
                ZipArchiveEntry zipEntry = new ZipArchiveEntry(file.getFileName());
                zipEntry.setSize(file.getBytes().length);

                zipOutputStream.putArchiveEntry(zipEntry);
                IOUtils.copy(new ByteArrayInputStream(file.getBytes()), zipOutputStream);
                zipOutputStream.closeArchiveEntry();
            }
            zipOutputStream.finish();
            zipOutputStream.close();

            return new Archive(archive.toByteArray());
        }
        catch (IOException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        finally {
            IOUtils.closeQuietly(archive);
            IOUtils.closeQuietly(zipOutputStream);
        }
    }
}
