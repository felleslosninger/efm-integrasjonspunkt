package no.difi.meldingsutveksling.config;

import com.google.common.collect.Sets;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

import java.util.Set;

@Data
public class AltinnFormidlingsTjenestenConfig {

    private String brokerserviceUrl;
    private String altinnTokenExchangeUrl;
    private String resource;

    private Integer connectTimeout;
    private Integer requestTimeout;

    @NotNull
    private DataSize uploadSizeLimit;

    @Pattern(regexp = "^[a-zA-Z0-9-_]{0,25}$")
    private String messageChannel;

    @NotNull
    private Integer defaultTtlHours;

    @Valid
    @NestedConfigurationProperty
    private Oidc oidc;

    /**
     * Altinn systemnavn (systemName) og url til systemregisteret (systemRegisterUrl), benyttes for onboarding av DPO
     */
    @NotNull
    private String systemName;

    @NotNull
    private String systemRegisterUrl;

    /**
     * Altinn systembruker (systemUser) benyttes både ved onboarding DPO og ved senere bruk
     */
    @NestedConfigurationProperty
    private AltinnSystemUser systemUser;

    /**
     * Altinn liste av ekstra systembrukere (reportees) som det også skal behandles meldinger for
     */
    private Set<AltinnSystemUser> reportees = Sets.newHashSet();

}
