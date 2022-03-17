package no.difi.meldingsutveksling.cucumber;

import lombok.SneakyThrows;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Profile("cucumber")
class ZipParser {

    @SneakyThrows
    ZipContent parse(Resource zip) {
        ZipContent zipContent = new ZipContent();

        try (ZipInputStream stream = new ZipInputStream(zip.getInputStream())) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                zipContent.file(Document.builder()
                        .filename(entry.getName())
                        .resource(new ByteArrayResource(StreamUtils.copyToByteArray(stream)))
                        .mimeType(MediaType.APPLICATION_OCTET_STREAM)
                        .build());
            }
        }

        return zipContent;
    }
}
