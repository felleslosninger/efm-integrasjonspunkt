package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.AltinnFormidlingsTjenestenConfig;
import java.net.URL;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import no.difi.meldingsutveksling.ptp.DigitalPostInnbyggerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

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

    /**
     * Feature toggles.
     */
    private FeatureToggle feature;

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
        @NotNull
        private String endpointUrl;

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

        private boolean returnOkOnEmptyPayload;
    }

}
