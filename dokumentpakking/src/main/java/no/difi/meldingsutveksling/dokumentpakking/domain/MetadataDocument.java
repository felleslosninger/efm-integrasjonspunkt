package no.difi.meldingsutveksling.dokumentpakking.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

@Value
@Builder
public class MetadataDocument implements AsicEAttachable {

    @NonNull String filename;
    @NonNull Resource resource;
    @NonNull MimeType mimeType;
    String title;

    public static class MetadataDocumentBuilder {
        private MimeType mimeType;

        public MetadataDocumentBuilder mimeType(MimeType mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public MetadataDocumentBuilder mimeType(String mimeType) {
            this.mimeType = mimeType != null ? MimeType.valueOf(mimeType) : null;
            return this;
        }
    }
}
