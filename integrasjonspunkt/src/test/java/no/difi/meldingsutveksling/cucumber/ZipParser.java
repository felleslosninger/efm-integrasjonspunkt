package no.difi.meldingsutveksling.cucumber;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Profile("cucumber")
class ZipParser {

    @SneakyThrows
    ZipContent parse(InputStream is) {
        ZipContent zipContent = new ZipContent();

        try (ZipInputStream stream = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                ZipFile zipFile = new ZipFile(entry.getName());

                try (OutputStream os = zipFile.getOutputStream()) {
                    IOUtils.copy(stream, os);
                    os.flush();
                }

                zipContent.files(zipFile);
            }
        }

        return zipContent;
    }
}
