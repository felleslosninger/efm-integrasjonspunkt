package no.difi.meldingsutveksling.ks.svarinn;

import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SvarInnUnzipper {
    public List<SvarInnFile> unzip(SvarInnFile svarInnZipFile) throws IOException {
        final ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(svarInnZipFile.getContents()));


        ZipEntry zipEntry;
        List<SvarInnFile> decompressedFiles = new ArrayList<>();
        while ((zipEntry = inputStream.getNextEntry()) != null) {
            if(zipEntry.isDirectory())
                continue;
            byte[] buffer = new byte[2048];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(buffer.length);

            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            decompressedFiles.add(new SvarInnFile(MediaType.ALL, outputStream.toByteArray()));
            outputStream.close();
        }

        return decompressedFiles;
    }
}
