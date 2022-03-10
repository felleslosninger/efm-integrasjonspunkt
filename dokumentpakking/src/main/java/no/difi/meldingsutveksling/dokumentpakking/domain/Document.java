package no.difi.meldingsutveksling.dokumentpakking.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

@Value
@Builder
public class Document implements AsicEAttachable {

    @NonNull String filename;
    @NonNull Resource resource;
    @With
    @NonNull MimeType mimeType;
    String title;
    MetadataDocument metadataDocument;

    public static class DocumentBuilder {
        private MimeType mimeType;

        public DocumentBuilder mimeType(MimeType mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public DocumentBuilder mimeType(String mimeType) {
            this.mimeType = mimeType != null ? MimeType.valueOf(mimeType) : null;
            return this;
        }
    }
}
