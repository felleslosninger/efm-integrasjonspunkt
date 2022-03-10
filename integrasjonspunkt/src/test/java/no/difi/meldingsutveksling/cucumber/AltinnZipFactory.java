package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.asic.SignatureHelper;
import no.difi.asic.SignatureMethod;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsAlgorithm;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSEncryptedAsice;
import no.difi.meldingsutveksling.nextmove.ManifestFactory;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.ResourceUtils;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class AltinnZipFactory {

    private final ObjectMapper objectMapper;
    private final AsicHandler asicHandler;
    private final KeystoreHelper keystoreHelper;
    private final CreateCMSEncryptedAsice createCMSEncryptedAsice;

    public ByteArrayResource createAltinnZip(Message message) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bos);
        out.putNextEntry(new ZipEntry(SBD_FILE));
        out.write(objectMapper.writeValueAsString(message.getSbd()).getBytes());
        out.closeEntry();

        out.putNextEntry(new ZipEntry(ASIC_FILE));

        asicHandler.createEncryptedAsic(NextMoveMessage.of());

        return new ByteArrayResource(createCMSEncryptedAsice.toByteArray(CreateCMSEncryptedAsice.Input.builder()
                        .manifest()
                        .documents()
                        .signatureHelper(keystoreHelper.getSignatureHelper())
                        .certificate(keystoreHelper.getX509Certificate())
                        .signatureMethod(SignatureMethod.CAdES)
                        .keyEncryptionScheme(CmsAlgorithm.RSAES_OAEP)
                .build()));


        @NonNull Manifest manifest;
        @NonNull Stream<AsicEAttachable> documents;
        @NonNull X509Certificate certificate;
        @NonNull SignatureMethod signatureMethod;
        @NonNull SignatureHelper signatureHelper;
        AlgorithmIdentifier keyEncryptionScheme;
        @Builder.Default String tempFilePrefix = "";

        ResourceUtils.copy(cmsEncryptedAsice, out);

        out.closeEntry();
        out.close();

        return bos.toByteArray();
    }
}
