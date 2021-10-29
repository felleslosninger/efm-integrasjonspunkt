package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dpi.client.domain.Document;
import no.difi.meldingsutveksling.dpi.client.domain.MetadataDocument;
import no.difi.meldingsutveksling.dpi.client.domain.Parcel;
import no.difi.meldingsutveksling.dpi.client.sdp.SDPDokument;
import no.difi.meldingsutveksling.dpi.client.sdp.SDPDokumentData;
import no.difi.meldingsutveksling.dpi.client.sdp.SDPManifest;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ParcelParser {

    private final AsicParser asicParser;
    private final ManifestParser manifestParser;
    private final DocumentStorage documentStorage;

    public Parcel parse(String messageId, InputStream asicInputStream) {
        Map<String, Document> documents = new HashMap<>();
        asicParser.parse(asicInputStream,
                ((filename, inputStream) -> {
                    documentStorage.write(messageId, filename, inputStream);
                    Resource resource = documentStorage.read(messageId, filename);
                    documents.put(filename, getDocument(filename, resource));
                }));

        SDPManifest manifest = getSdpManifest(documents);

        return new Parcel()
                .setMainDocument(getDocument(documents, manifest.getHoveddokument()))
                .setAttachments(manifest.getVedleggs().stream()
                        .map(p -> getDocument(documents, p))
                        .collect(Collectors.toList())
                );

    }

    private SDPManifest getSdpManifest(Map<String, Document> documents) {
        return manifestParser.parse(
                getDocument(documents, "manifest.xml")
                        .getResource());
    }

    private Document getDocument(Map<String, Document> documents, String filename) {
        return Optional.ofNullable(documents.get(filename))
                .orElseThrow(() -> new Exception(String.format("No file named '%s' in ASICe!", filename)));
    }

    private Document getDocument(Map<String, Document> documents, SDPDokument sdpDokument) {
        return getDocument(documents, sdpDokument.getHref())
                .setMimeType(sdpDokument.getMime())
                .setTitle(sdpDokument.getTittel().getValue())
                .setMetadataDocument(getMetadataDocument(sdpDokument.getData()));
    }

    private MetadataDocument getMetadataDocument(SDPDokumentData data) {
        if (data == null) {
            return null;
        }
        return new MetadataDocument()
                .setFilename(data.getHref())
                .setMimeType(data.getMime());
    }

    private Document getDocument(String filename, Resource resource) {
        return new Document()
                .setFilename(filename)
                .setResource(resource);
    }

    private static class Exception extends RuntimeException {
        public Exception(String message) {
            super(message);
        }
    }
}
