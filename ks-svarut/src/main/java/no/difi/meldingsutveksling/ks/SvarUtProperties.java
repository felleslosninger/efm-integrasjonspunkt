package no.difi.meldingsutveksling.ks;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;
import java.net.URI;

@Data
@ConfigurationProperties(prefix="difi.move.ks")
public class SvarUtProperties {
    /**
     * Uri to the web service
     */
    @NotNull
    private URI url;


}
