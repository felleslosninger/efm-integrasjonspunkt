package no.difi.meldingsutveksling.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

import java.net.URL;

@Data
public class PostVirksomheter {

    private URL endpointUrl;
    @NotNull
    private String sensitiveResource;
    private boolean notifyEmail;
    private boolean notifySms;
    @NotNull
    private String notificationText;
    @NotNull
    private String sensitiveNotificationText;
    private boolean allowForwarding;
    private boolean enableDueDate;
    private Long daysToReply;
    @NotNull
    private DataSize uploadSizeLimit;
    @NotNull
    private Integer defaultTtlHours;
    private String correspondenceServiceUrl;
    private String healthCheckUrl;
    private String altinnTokenExchangeUrl;
    @Valid
    private Oidc oidc;

    @Data
    public static class Oidc {
        private URL url;
        private String audience;
        private String clientId;
        @NestedConfigurationProperty
        private KeystoreProperties keystore;
    }
}
