package no.difi.meldingsutveksling.config;

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.ToString;
import org.springframework.util.unit.DataSize;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Set;

@Data
@ToString(exclude = "password")
public class AltinnFormidlingsTjenestenConfig {

    /**
     * System user username for altinn.
     */
    private String username;
    /**
     * System user password for altinn;
     */
    private String password;

    private String streamingserviceUrl;
    private String brokerserviceUrl;
    private String serviceCode;
    private String serviceEditionCode;
    private Integer connectTimeout;
    private Integer requestTimeout;
    @NotNull
    private DataSize uploadSizeLimit;
    @Pattern(regexp = "^[a-zA-Z0-9-_]{0,25}$")
    private String messageChannel;
    private Set<String> reportees = Sets.newHashSet();

}
