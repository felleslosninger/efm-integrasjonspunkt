package no.difi.meldingsutveksling.cucumber;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
class ZipParser {

    @SneakyThrows
    ZipContent parse(InputStream is) {
        ZipContent zipContent = new ZipContent();

        try (ZipInputStream stream = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                zipContent.files(new ZipFile()
                        .setFileName(entry.getName())
                        .setBytes(IOUtils.toByteArray(stream))
                );
            }
        }

        return zipContent;
    }
}
