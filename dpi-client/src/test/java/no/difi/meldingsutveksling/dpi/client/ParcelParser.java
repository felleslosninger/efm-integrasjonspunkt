package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.domain.MetadataDocument;
import no.difi.meldingsutveksling.dokumentpakking.domain.Parcel;
import no.difi.meldingsutveksling.dokumentpakking.service.AsicParser;
import no.difi.meldingsutveksling.dpi.client.sdp.SDPDokument;
import no.difi.meldingsutveksling.dpi.client.sdp.SDPDokumentData;
import no.difi.meldingsutveksling.dpi.client.sdp.SDPManifest;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ParcelParser {

    private final AsicParser asicParser;
    private final ManifestParser manifestParser;
    private final DocumentStorage documentStorage;

    public Parcel parse(String messageId, Resource asic) {
        Map<String, InternalDocument> documents = new HashMap<>();
        asicParser.parse(asic,
                ((filename, inputStream) -> {
                    documentStorage.write(messageId, filename, inputStream);
                    Resource resource = documentStorage.read(messageId, filename);
                    documents.put(filename, new InternalDocument(filename, resource));
                }));

        SDPManifest manifest = getSdpManifest(documents);

        return Parcel.builder()
                .mainDocument(getDocument(documents, manifest.getHoveddokument()))
                .attachments(manifest.getVedleggs().stream()
                        .map(p -> getDocument(documents, p))
                        .collect(Collectors.toList()))
                .build();
    }

    private SDPManifest getSdpManifest(Map<String, InternalDocument> documents) {
        return manifestParser.parse(
                getDocument(documents, "manifest.xml")
                        .getResource());
    }

    private InternalDocument getDocument(Map<String, InternalDocument> documents, String filename) {
        return Optional.ofNullable(documents.get(filename))
                .orElseThrow(() -> new IllegalArgumentException(String.format("No file named '%s' in ASICe!", filename)));
    }

    private Document getDocument(Map<String, InternalDocument> documents, SDPDokument sdpDokument) {
        InternalDocument document = getDocument(documents, sdpDokument.getHref());
        return Document.builder()
                .filename(document.getFilename())
                .resource(document.getResource())
                .mimeType(sdpDokument.getMime())
                .title(sdpDokument.getTittel().getValue())
                .metadataDocument(getMetadataDocument(sdpDokument.getData()))
                .build();
    }

    private MetadataDocument getMetadataDocument(SDPDokumentData data) {
        if (data == null) {
            return null;
        }
        return MetadataDocument.builder()
                .filename(data.getHref())
                .mimeType(data.getMime())
                .build();
    }

    @Value
    private static class InternalDocument {
        String filename;
        Resource resource;
    }
}
