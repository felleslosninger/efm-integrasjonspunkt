package no.difi.meldingsutveksling.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

import java.net.URL;

@Data
public class PostVirksomheter {

    @NotNull
    private String sensitiveResource;

    private boolean notifyEmail;
    private boolean notifySms;

    @NotNull
    private String notificationText;

    @NotNull
    private String sensitiveNotificationText;

    @NotNull
    private String emailSubject;

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
    @NestedConfigurationProperty
    private Oidc oidc;

}
