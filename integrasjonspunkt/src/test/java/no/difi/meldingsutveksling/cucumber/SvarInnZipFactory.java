package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsAlgorithm;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSDocument;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.ResourceUtils;
import no.difi.move.common.io.WritableByteArrayResource;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class SvarInnZipFactory {

    private final CreateCMSDocument createCMSDocument;
    private final KeystoreHelper fiksKeystoreHelper;

    @SneakyThrows
    ByteArrayResource createSvarInnZip(Message message) {
        WritableByteArrayResource resource = new WritableByteArrayResource();

        try (ZipOutputStream out = new ZipOutputStream(resource.getOutputStream())) {
            for (Document file : message.getAttachments()) {
                out.putNextEntry(new ZipEntry(file.getFilename()));
                ResourceUtils.copy(file.getResource(), out);
                out.closeEntry();
            }
        }

        return encrypt(resource);
    }

    private ByteArrayResource encrypt(Resource resource) {
        WritableByteArrayResource writableByteArrayResource = new WritableByteArrayResource();
        createCMSDocument.encrypt(CreateCMSDocument.Input.builder()
                .resource(resource)
                .certificate(fiksKeystoreHelper.getX509Certificate())
                .keyEncryptionScheme(CmsAlgorithm.RSAES_OAEP)
                .build(), writableByteArrayResource);
        return new ByteArrayResource(writableByteArrayResource.toByteArray());
    }
}
