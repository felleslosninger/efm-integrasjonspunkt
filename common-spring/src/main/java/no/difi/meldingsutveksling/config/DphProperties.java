package no.difi.meldingsutveksling.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.net.URL;
import java.util.List;

@Data
public class DphProperties {

    @NotNull
    private Integer defaultTtlHours;

    private List<@NotNull Integer> herIds;

    @NotNull
    private String nhnProcess;
    @NotNull
    private String dialogmeldingDocumentType;
    @NotNull
    private String receiptProcess;
    @NotNull
    private String receiptDocumentType;

    private DataSize uploadSizeLimit = DataSize.ofMegabytes(18);
    private DataSize temporaryFileThreshold = DataSize.ofMegabytes(10);
    private File temporaryFileDirectory;
    private DataSize initialBufferSize = DataSize.ofKilobytes(64);

    @Positive
    private long pollingrate = 30000;

    @NotNull
    private String uri;

    @NotNull
    @Valid
    @NestedConfigurationProperty
    private Timeout timeout;

    @Valid
    @NotNull
    @NestedConfigurationProperty
    private Certificate certificate;

    @Valid
    @NotNull
    @NestedConfigurationProperty
    private Oidc oidc;

    @Valid
    @NestedConfigurationProperty
    private KeystoreProperties keystore;

    @Data
    public static class Oidc {

        @NotNull
        private boolean enable;
        private URL url;
        private String audience;
        private List<String> scopes;
        @NestedConfigurationProperty
        private KeystoreProperties keystore;
        private Mock mock;
        private String clientId;

        @Data
        public static class Mock {
            private String token;
        }
    }

    @Data
    public static class Timeout {
        private int connect;
        private int read;
        private int write;
    }

    @Data
    public static class Certificate {

        @NotNull
        private String mode;
    }
}
