package no.difi.meldingsutveksling.altinnv3.proxy.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("altinn")
@Validated
public record AltinnProperties(
    @NotNull
    String baseUrl,
    @NotNull
    String accessList,
    @NotNull
    String accessListOwner
) {
}
