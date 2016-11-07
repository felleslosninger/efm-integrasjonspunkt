package no.difi.meldingsutveksling.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.URL;

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
    private IdportenOidc idportenOidc;

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

    @Data
    public static class IdportenOidc {

        @NotNull(message = "ID-porten OIDC endpoint base url must be configured.")
        private URL baseUrl;

        @NotNull(message = "ID-porten OIDC issuer must be configured.")
        private String issuer;
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
    }

}
