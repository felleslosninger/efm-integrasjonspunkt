package no.difi.meldingsutveksling.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.URL;
import java.util.List;

/**
 * Configurable properties for Integrasjonspunkt.
 *
 * @author kons-nlu
 */
@Data
@ConfigurationProperties(prefix = "difi.move")
public class IntegrasjonspunktProperties {

    @Valid
    private Organization org;

    /**
     * Service registry endpoint.
     */
    @NotNull(message = "Service registry must be configured")
    private String serviceregistryEndpoint;

    @Valid
    private AltinnFormidlingsTjenestenConfig altinn;

    @Valid
    private PostVirksomheter altinnPTV;

    @Valid
    private NorskArkivstandardSystem noarkSystem;

    @Valid
    private MessageServiceHandler msh;

    @Valid
    private DigitalPostInnbyggerConfig dpi;

    @Valid
    private SvarUtConfig dps;

    @Valid
    private Oidc oidc;

    private Mail mail;

    @Valid
    private NextBEST nextbest;

    /**
     * Use this parameter to indicate that the message are related to vedtak/messages that require the recipient to be
     * notified. This parameter is passed over to ServiceRegistry to determine where the message should be sent.
     * (See http://begrep.difi.no/SikkerDigitalPost/1.2.3/forretningslag/varsling for more information)
     */
    private boolean varslingsplikt = false;

    /**
     * Feature toggles.
     */
    @Valid
    private FeatureToggle feature;

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
        private Keystore keystore;
    }

    @Data
    public static class PostVirksomheter {

        private String username;
        private String password;
        /**
         * TODO: descrive
         */
        private String externalServiceCode;
        /**
         * TODO: descrive
         */
        private String externalServiceEditionCode;

    }

    /**
     * Idporten Oidc
     */
    @Data
    public static class Oidc {

        @NotNull
        private boolean enable;
        private URL url;
        private List<String> scopes;
        private String clientId;
        private Keystore keystore;
    }

    /**
     * Mail settings for
     */
    @Data
    public static class Mail {

        private String smtpHost;
        private String smtpPort;
        private String senderAddress;
        private String receiverAddress;
        private String enableAuth;
        private String username;
        private String password;
        private String trust;
    }

    /**
     * Settings for NextBEST
     */
    @Data
    public static class NextBEST {

        @NotNull
        private String filedir;

        @NotNull
        private String asicfile;
    }

    @Data
    public static class NorskArkivstandardSystem {

        @NotNull
        private String endpointURL;
        private String username;
        private String password;
        /**
         * TODO: descrive
         */
        private String domain;
        /**
         * TODO: descrive
         */
        @NotNull
        private String type;

    }

    @Data
    public static class MessageServiceHandler {

        private String username;
        private String password;
        private String endpointURL;

    }

    @Data
    public static class Keystore {

        /**
         * Keystore alias for key.
         */
        @NotNull
        private String alias;
        /**
         * Path of jks file.
         */

        @NotNull
        private Resource path;
        /**
         * Password of keystore and entry.
         */
        @NotNull
        private String password;

    }

    @Data
    public static class FeatureToggle {

        /**
         * Activate new internal queue.
         */
        private boolean enableQueue;

        private boolean enableReceipts;

        private boolean returnOkOnEmptyPayload;
        private boolean enableDpiReceipts;

        public boolean isEnableDpiReceipts() {
            return enableDpiReceipts;
        }
    }

}
