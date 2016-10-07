package no.difi.meldingsutveksling.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import no.difi.meldingsutveksling.ptp.DigitalPostInnbyggerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable properties for Integrasjonspunkt.
 *
 * @author kons-nlu
 */
@Data
@ConfigurationProperties(prefix = "difi.miif")
public class IntegrasjonspunktProperties {

    /**
     * Organization number to run as.
     */
    @NotNull(message = "difi.miif.orgnumber is not set. This property is required.")
    @Size(min = 9, max = 9, message = "difi.miif.orgnumber must be exactly 9 digits")
    private String orgnumber;

    /**
     * Service registry endpoint.
     */
    private String serviceregistryEndpoint;

    /**
     * Feature toggles.
     */
    private FeatureToggle feature;

    /**
     * Business certificate for this instance.
     */
    @Valid
    @NotNull(message = "Certificate properties not set.")
    private Certificate cert;

    @Valid
    private Altinn altinn;

    @Valid
    private DigitalPostInnbyggerConfig dpi;

    @Valid
    private PostVirksomheter altinnptv;

    @Valid
    private NorskArkivstandardSystem noarkSystem;

    @Valid
    private MessageServiceHandler msh;

    @Data
    public static class Altinn {

        /**
         * System user username for altinn.
         */
        private String username;
        /**
         * System user password for altinn;
         */
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
        private String endpointUrl;

    }

    @Data
    public static class NorskArkivstandardSystem {

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
        private String type;

    }

    @Data
    public static class Certificate {

        /**
         * Keystore alias for key.
         */
        @NotNull
        private String alias;
        /**
         * Path of jks file.
         */

        @NotNull
        private String path;
        /**
         * Password of keystore and entry.
         */
        @NotNull
        private String password;

    }

    @Data
    public static class MessageServiceHandler {

        private String username;
        private String password;
        private String endpointURL;

    }

    @Data
    public static class FeatureToggle {

        /**
         * Activate new internal queue.
         */
        private boolean enableQueue;

        private boolean returnOkOnEmptyPayload;
    }

}
