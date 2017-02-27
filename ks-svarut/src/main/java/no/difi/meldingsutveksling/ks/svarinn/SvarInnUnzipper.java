package no.difi.meldingsutveksling.ks.svarinn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SvarInnUnzipper {
    public Map<String, byte[]> unzip(byte[] zipFile) throws IOException {
        final ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(zipFile));


        ZipEntry zipEntry;
        Map<String, byte[]> decompressedFiles = new HashMap<>();
        while ((zipEntry = inputStream.getNextEntry()) != null) {
            if(zipEntry.isDirectory())
                continue;
            byte[] buffer = new byte[2048];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(buffer.length);

            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            decompressedFiles.put(zipEntry.getName(), outputStream.toByteArray());
            outputStream.close();
        }

        return decompressedFiles;
    }
}
