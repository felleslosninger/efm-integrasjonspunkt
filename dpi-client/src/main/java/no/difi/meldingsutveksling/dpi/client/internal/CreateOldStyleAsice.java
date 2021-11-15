package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.dpi.client.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.meldingsutveksling.dpi.client.internal.domain.Manifest;
import no.difi.meldingsutveksling.dpi.client.internal.domain.Signature;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class CreateOldStyleAsice implements CreateAsice {

    private final CreateManifest createManifest;
    private final CreateSignature createSignature;
    private final CreateZip createZip;

    public void createAsice(Shipment shipment, OutputStream outputStream) {
        // Lag ASiC-E manifest
        log.info("Creating ASiC-E manifest");
        Manifest manifest = createManifest.createManifest(shipment);

        List<AsicEAttachable> files = new ArrayList<>();
        files.add(manifest);
        files.add(shipment.getParcel().getMainDocument());
        files.addAll(shipment.getParcel().getAttachments());
        Optional.ofNullable(shipment.getParcel().getMainDocument().getMetadataDocument()).ifPresent(files::add);

        // Lag signatur over alle filene i pakka
        Signature signature = createSignature.createSignature(files);
        files.add(signature);

        try (InputStream is = signature.getResource().getInputStream()) {
            log.debug("ASiC-E signature.xml: {}", IOUtils.toString(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new Exception("Could not dump signature.xml", e);
        }

        // Zip filene
        log.trace("Zipping ASiC-E files. Contains a total of " + files.size() + " files (including the generated manifest and signatures)");
        createZip.zipIt(files, outputStream);
    }

    private static class Exception extends RuntimeException {
        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
