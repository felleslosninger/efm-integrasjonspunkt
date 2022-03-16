package no.difi.meldingsutveksling.ks.mapping.edu;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.FiksConfig;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsAlgorithm;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.move.common.io.WritableByteArrayResource;
import org.springframework.core.io.ByteArrayResource;

import java.security.cert.X509Certificate;
import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public class FileTypeHandlerFactory {
    private final FiksConfig fiksConfig;
    private final X509Certificate certificate;
    private final CreateCMSDocument createCMSDocument;

    public FileTypeHandler createFileTypeHandler(DokumentType dokumentType) {
        return new FileTypeHandler(dokumentType, getTransform());
    }

    private UnaryOperator<byte[]> getTransform() {
        return fiksConfig.isKryptert() ? this::encrypt : p -> p;
    }

    private byte[] encrypt(byte[] bytes) {
        WritableByteArrayResource encrypted = new WritableByteArrayResource();
        createCMSDocument.encrypt(CreateCMSDocument.Input.builder()
                .resource(new ByteArrayResource(bytes))
                .certificate(certificate)
                .keyEncryptionScheme(CmsAlgorithm.RSAES_OAEP)
                .build(), encrypted);
        return encrypted.toByteArray();
    }
}
