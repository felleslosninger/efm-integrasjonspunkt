package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.*;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.move.common.io.OutputStreamResource;
import no.difi.move.common.io.pipe.Pipe;
import no.difi.move.common.io.pipe.PipeResource;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.Reject;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class CreateAsice {

    private final Plumber plumber;

    public Resource createAsice(Input input, Reject reject) {
        Pipe pipe = plumber.pipe("Creating ASiC-E",
                inlet -> createAsice(input, new OutputStreamResource(inlet)),
                reject);

        return new PipeResource(pipe, "Creating ASiC-E");
    }

    public void createAsice(Input input, WritableResource output) {
        log.info("Creating ASiC-E manifest");
        AsicWriter asicWriter = getAsicWriter(input, output);
        addAsicFile(asicWriter, input.getManifest());
        input.getDocuments().forEachOrdered(p -> addAsicFile(asicWriter, p));
        sign(input, asicWriter);
    }

    private void sign(Input input, AsicWriter asicWriter) {
        try {
            asicWriter.sign(input.getSignatureHelper());
        } catch (IOException e) {
            throw new IllegalStateException("Could not sign ASiC-E!", e);
        }
    }

    private void addAsicFile(AsicWriter asicWriter, AsicEAttachable attachable) {
        log.debug("Adding file {} of type {}", attachable.getFilename(), attachable.getMimeType());
        try (InputStream inputStream = new BufferedInputStream(attachable.getResource().getInputStream())) {
            asicWriter.add(inputStream, attachable.getFilename(), MimeType.forString(attachable.getMimeType().toString()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not add manifest to ASiC-E!", e);
        }
    }

    @SneakyThrows({IOException.class})
    private AsicWriter getAsicWriter(Input input, WritableResource output) {
        return AsicWriterFactory.newFactory(input.getSignatureMethod())
                .newContainer(output.getOutputStream());
    }

    @Value
    @Builder
    public static class Input {
        @NonNull Manifest manifest;
        @NonNull Stream<? extends AsicEAttachable> documents;
        @NonNull X509Certificate certificate;
        @NonNull SignatureMethod signatureMethod;
        @NonNull SignatureHelper signatureHelper;
        @Builder.Default
        String tempFilePrefix = "";
    }
}

