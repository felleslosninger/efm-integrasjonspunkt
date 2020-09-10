package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.extern.slf4j.Slf4j;
import no.difi.asic.AsicWriter;
import no.difi.asic.AsicWriterFactory;
import no.difi.asic.MimeType;
import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.StreamedFile;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class CreateAsice {

    private final ManifestFactory manifestFactory;

    public CreateAsice() {
        manifestFactory = new ManifestFactory();
    }

    public void createAsiceStreamed(StreamedFile mainAttachment,
                                    Stream<? extends StreamedFile> files,
                                    OutputStream archive,
                                    SignatureHelper signatureHelper,
                                    NextMoveMessage message) throws IOException {
        Manifest manifest = manifestFactory.createManifest(message, mainAttachment.getFileName(), mainAttachment.getMimeType());
        AsicWriter asicWriter = AsicWriterFactory.newFactory()
                .newContainer(archive)
                .add(new BufferedInputStream(new ByteArrayInputStream(manifest.getBytes())), "manifest.xml", MimeType.XML);

        List<InputStream> streamsToClose = new ArrayList<>();

        try {
            files.forEach(f -> {
                try {
                    log.debug("Adding file {} of type {}", f.getFileName(), f.getMimeType());
                    InputStream inputStream = new BufferedInputStream(f.getInputStream());
                    streamsToClose.add(inputStream);
                    asicWriter.add(inputStream, f.getFileName(), MimeType.forString(f.getMimeType()));
                } catch (IOException e) {
                    throw new MeldingsUtvekslingRuntimeException(StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT.getTechnicalMessage(), e);
                }
            });
            asicWriter.sign(signatureHelper);
        } finally {
            for (InputStream is : streamsToClose) {
                is.close();
            }
        }
    }
}
