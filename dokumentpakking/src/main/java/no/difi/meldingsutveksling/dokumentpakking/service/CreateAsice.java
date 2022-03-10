package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.*;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import no.difi.move.common.io.OutputStreamResource;
import no.difi.move.common.io.WritableByteArrayResource;
import org.springframework.core.io.InputStreamResource;
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
    private final InMemoryWithTempFileFallbackResourceFactory resourceFactory;

    public byte[] toByteArray(Input input) {
        WritableByteArrayResource output = new WritableByteArrayResource();
        createAsice(input, output);
        return output.toByteArray();
    }

    public InMemoryWithTempFileFallbackResource createAsice(Input input) {
        InMemoryWithTempFileFallbackResource output = resourceFactory.getResource(input.getTempFilePrefix(), ".asic");
        createAsice(input, output);
        return output;
    }

    public InputStreamResource createAsice(Input input, Reject reject) {
        return new InputStreamResource(plumber.pipe("Creating Asice", inlet -> {
            try {
                createAsice(input, new OutputStreamResource(inlet));
            } catch (Exception e) {
                reject.reject(new IllegalStateException("Couldn't create Asice!", e));
            }
        }, reject).outlet());
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
        @NonNull Stream<AsicEAttachable> documents;
        @NonNull X509Certificate certificate;
        @NonNull SignatureMethod signatureMethod;
        @NonNull SignatureHelper signatureHelper;
        @Builder.Default
        String tempFilePrefix = "";
    }
}

