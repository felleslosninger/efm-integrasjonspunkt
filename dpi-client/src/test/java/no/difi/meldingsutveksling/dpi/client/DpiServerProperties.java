package no.difi.meldingsutveksling.dpi.client;

import lombok.Data;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.Valid;

@Data
@ConfigurationProperties(prefix = "dpi.server")
public class DpiServerProperties {

    @Valid
    private KeystoreProperties keystore;
}
