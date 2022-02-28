package no.difi.meldingsutveksling.dpi.client;

import lombok.Data;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URL;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "dpi.client")
public class DpiClientProperties {

    @NotNull
    private String type;

    @NotNull
    private String uri;

    @NotNull
    private String schema;

    @Valid
    @NotNull
    private Timeout timeout;

    @Valid
    @NotNull
    private Certificate certificate;

    private int temporaryFileThreshold = 10 * 1000 * 1000;
    private File temporaryFileDirectory;
    private int initialBufferSize = 100000;

    @Valid
    private KeystoreProperties keystore;

    @Valid
    private Oidc oidc;

    @Valid
    private Asice asice;

    @Data
    public static class Asice {

        private String type;
    }

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