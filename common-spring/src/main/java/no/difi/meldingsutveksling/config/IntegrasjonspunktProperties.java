package no.difi.meldingsutveksling.config;

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.ToString;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.properties.LoggedProperty;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URL;
import java.util.Set;

/**
 * Configurable properties for Integrasjonspunkt.
 *
 * @author kons-nlu
 */
@Data
@ConfigurationProperties(prefix = "difi.move")
@Validated
public class IntegrasjonspunktProperties {

    @Valid
    private Organization org;

    @Valid
    @LoggedProperty
    private FeatureToggle feature;

    @NotNull(message = "Service registry must be configured")
    private String serviceregistryEndpoint;

    @Valid
    @NestedConfigurationProperty
    private AltinnFormidlingsTjenestenConfig dpo;

    @Valid
    private Arkivmelding arkivmelding;

    @Valid
    private Avtalt avtalt;

    @Valid
    private Einnsyn einnsyn;

    @Valid
    @NestedConfigurationProperty
    private PostVirksomheter dpv;

    @Valid
    @NestedConfigurationProperty
    private DigitalPostInnbyggerConfig dpi;

    @Valid
    @NestedConfigurationProperty
    private FiksConfig fiks = new FiksConfig();

    @Valid
    private Oidc oidc;

    private Mail mail;

    @Valid
    @LoggedProperty
    private NextMove nextmove;

    @Valid
    private WebHooks webhooks;

    @Valid
    private Sign sign;

    @Valid
    private Ntp ntp;

    @Valid
    private Queue queue;

    @Valid
    private DeadLock deadlock;

    @Data
    public static class Vault {
        private String uri;
        private String token;
        private String path;
    }

    @Data
    public static class Arkivmelding {
        @NotNull
        private String defaultProcess;
        @NotNull
        private String dpvDefaultProcess;
        @NotNull
        private String defaultDocumentType;
        @NotNull
        private String receiptProcess;
        @NotNull
        private String receiptDocumentType;
        private boolean generateReceipts;
    }

    @Data
    public static class Avtalt {
        @NotNull
        private String receiptProcess;
    }

    @Data
    public static class Einnsyn {
        @NotNull
        private String defaultJournalProcess;
        @NotNull
        private String defaultJournalDocumentType;
        @NotNull
        private String defaultInnsynskravProcess;
        @NotNull
        private String defaultInnsynskravDocumentType;
        private String receiptProcess;

    }

    @Data
    public static class Ntp {
        @NotNull
        private String host;
        private boolean disable;

    }

    @Data
    public static class Queue {
        @NotNull
        private Integer maximumRetryHours;
        @NotNull
        private Integer concurrency;
        @NotNull
        private String nextmoveName;
        @NotNull
        private String dlqName;
    }

    public FeatureToggle getFeature() {
        if (this.feature == null) {
            this.feature = new FeatureToggle();
        }
        return this.feature;
    }

    @Data
    public static class Organization {

        /**
         * Organization number to run as.
         */
        @NotNull(message = "difi.move.org.number is not set. This property is required.")
        @Size(min = 9, max = 9, message = "difi.move.org.number must be exactly 9 digits")
        private String number;

        /**
         * Business certificate for this instance.
         */
        @Valid
        @NotNull(message = "Certificate properties not set.")
        @NestedConfigurationProperty
        private KeystoreProperties keystore;
    }

    /**
     * Idporten Oidc
     */
    @Data
    public static class Oidc {

        @NotNull
        private boolean enable;
        private URL url;
        private String audience;
        private String clientId;
        private String clientIdPrefix;
        @NestedConfigurationProperty
        private KeystoreProperties keystore;
    }

    /**
     * SR signing
     */
    @Data
    public static class Sign {

        private boolean enable;
        private URL jwkUrl;
    }

    /**
     * Mail settings for
     */
    @Data
    @ToString(exclude = "password")
    public static class Mail {

        private String smtpHost;
        private String smtpPort;
        private String senderAddress;
        private String receiverAddress;
        private String enableAuth;
        private String username;
        private String password;
        private String trust;
        private Long maxSize;
    }

    @Data
    public static class NextMove {

        @NotNull
        private String filedir;
        @NotNull
        private Integer lockTimeoutMinutes;
        @NotNull
        private Integer defaultTtlHours;
        @NotNull
        private Boolean applyZipHeaderPatch = Boolean.FALSE;
        @Valid
        private ServiceBus serviceBus;
        @NotNull
        private Set<ServiceIdentifier> statusServices;
        @NotNull
        private String statusDocumentType;
        @NotNull
        private Integer statusPollingPageSize;
        @NotNull
        private Boolean useDbPersistence;
    }

    @Data
    public static class WebHooks {

        @NotNull
        private Integer connectTimeout;
        @NotNull
        private Integer readTimeout;
    }

    @Data
    @ToString(exclude = "sasToken")
    public static class ServiceBus {
        @NotNull
        private String sasKeyName;
        @NotNull
        private String sasToken;
        @Pattern(regexp = "innsyn|data|meeting", flags = Pattern.Flag.CASE_INSENSITIVE)
        private String mode;
        @NotNull
        private String baseUrl;
        @NotNull
        private boolean useHttps;
        private String receiptQueue;
        private Integer readMaxMessages;
        private boolean batchRead;
        private Integer connectTimeout;
        @NotNull
        private DataSize uploadSizeLimit;
        @NotNull
        private Integer defaultTtlHours;

    }

    @Data
    public static class FeatureToggle {
        private boolean enableDsfPrintLookup;
        private boolean enableReceipts;
        private boolean forwardReceivedAppReceipts;
        private boolean returnOkOnEmptyPayload;
        private boolean dumpDlqMessages;
        private boolean mailErrorStatus;
        private boolean retryOnDeadLock;
        private boolean cryptoMessagePersister;
        private Set<ServiceIdentifier> statusQueueIncludes = Sets.newHashSet();

        /**
         * Service toggles
         */
        private boolean enableDPO;
        private boolean enableDPV;
        private boolean enableDPI;
        private boolean enableDPF;
        private boolean enableDPFIO;
        private boolean enableDPE;

    }

    @Data
    public static class Sms {
        @Size(max = 160)
        private String varslingstekst;
    }

    @Data
    public static class DeadLock {

        /**
         * How many retries should be tried on deadlock
         **/
        @Positive
        private int retryCount = 5;

        /**
         * How big is delay between deadlock retry (in ms)
         **/
        @Positive
        private int delay = 1000;
    }
}
