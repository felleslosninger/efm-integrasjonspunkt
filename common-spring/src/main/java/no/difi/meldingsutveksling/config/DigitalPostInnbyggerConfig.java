/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.difi.meldingsutveksling.config;

import lombok.Data;
import no.difi.meldingsutveksling.config.dpi.dpi.PrintSettings;
import no.difi.meldingsutveksling.config.dpi.dpi.Priority;
import no.difi.meldingsutveksling.properties.LoggedProperty;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.io.Resource;
import org.springframework.util.unit.DataSize;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URL;
import java.util.List;

@Data
public class DigitalPostInnbyggerConfig {

    private String endpoint;

    @Valid
    @NestedConfigurationProperty
    private KeystoreProperties keystore;

    @Valid
    @NestedConfigurationProperty
    private KeystoreProperties trustStore;

    /**
     * ID for queue messages are sent to and their corresponding receipts can be retrieved from.
     * This is to avoid reading receipts from other applications that use the same service
     */
    @NotNull
    @LoggedProperty
    private String mpcId;

    /**
     * The number of concurrent message partition channels (MPCs) to send messages to and consume receipts from.
     * An integer increment is postfixed to the MPC id if the MPC concurrency is greater than 1.
     * <p>
     * MPC concurrency of 3 will use the following MPCs:
     * - {mpcId}-0
     * - {mpcId}-1
     * - {mpcId}-2
     */
    @NotNull
    private Integer mpcConcurrency;

    /**
     * This list overrides the mpcId + mpcConcurrency
     */
    @LoggedProperty
    private List<String> mpcIdListe;
    /**
     * ID for queue messages are sent to and their corresponding receipts can be retrieved from.
     * This is to avoid reading receipts from other applications that use the same service
     */
    private List<String> avsenderidentifikatorListe;

    @NotNull
    private Boolean pollWithoutAvsenderidentifikator;

    @NotNull
    private String language;

    @NotNull
    private Priority priority;

    @NotNull
    @NestedConfigurationProperty
    private PrintSettings printSettings;

    @NotNull
    private DataSize uploadSizeLimit;

    @NotNull
    private int clientMaxConnectionPoolSize;

    @NotNull
    private String krrPrintUrl;

    @NotNull
    private String clientType;

    private String receiptType;

    @NotNull
    private String c2Type;

    @NotNull
    private String uri;

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
    private Oidc oidc;

    @Valid
    private Asice asice;

    @Valid
    private Server server;

    @NotNull
    private Integer defaultTtlHours;

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
        private Resource recipe;
    }

    @Data
    public static class Server {

        @Valid
        private KeystoreProperties keystore;
    }

}
