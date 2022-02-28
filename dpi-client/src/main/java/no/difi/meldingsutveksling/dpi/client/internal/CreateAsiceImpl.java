package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.*;
import no.difi.meldingsutveksling.dpi.client.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.meldingsutveksling.dpi.client.internal.domain.Manifest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class CreateAsiceImpl implements CreateAsice {

    private final CreateManifest createManifest;
    private final SignatureHelper signatureHelper;

    public void createAsice(Shipment shipment, OutputStream outputStream) {
        log.info("Creating ASiC-E manifest");
        Manifest manifest = createManifest.createManifest(shipment);

        AsicWriter asicWriter = getAsicWriter(outputStream);

        addAsicFile(asicWriter, manifest);
        addAsicFile(asicWriter, shipment.getParcel().getMainDocument());
        shipment.getParcel().getAttachments().forEach(p -> addAsicFile(asicWriter, p));
        Optional.ofNullable(shipment.getParcel().getMainDocument().getMetadataDocument()).ifPresent(p -> addAsicFile(asicWriter, p));

        sign(asicWriter);
    }

    private void sign(AsicWriter asicWriter) {
        try {
            asicWriter.sign(signatureHelper);
        } catch (IOException e) {
            throw new RuntimeException("Could not sign ASiC-E!", e);
        }
    }

    private void addAsicFile(AsicWriter asicWriter, AsicEAttachable attachable) {
        log.debug("Adding file {} of type {}", attachable.getFilename(), attachable.getMimeType());
        try (InputStream inputStream = new BufferedInputStream(attachable.getResource().getInputStream())) {
            asicWriter.add(inputStream, attachable.getFilename(), MimeType.forString(attachable.getMimeType()));
        } catch (IOException e) {
            throw new RuntimeException("Could not add manifest to ASiC-E!", e);
        }
    }

    @SneakyThrows({IOException.class})
    private AsicWriter getAsicWriter(OutputStream outputStream) {
            return AsicWriterFactory.newFactory(SignatureMethod.XAdES)
                    .newContainer(outputStream);
    }
}
