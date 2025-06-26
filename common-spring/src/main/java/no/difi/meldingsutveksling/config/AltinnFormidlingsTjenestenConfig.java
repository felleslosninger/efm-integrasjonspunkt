package no.difi.meldingsutveksling.config;

import com.google.common.collect.Sets;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;
import org.springframework.util.unit.DataSize;

import java.util.Set;

@Data
@ToString(exclude = "password")
public class AltinnFormidlingsTjenestenConfig {

    private String streamingserviceUrl;
    private String brokerserviceUrl;
    private String serviceCode;
    private String serviceEditionCode;
    private String altinnTokenExchangeUrl;

    private Integer connectTimeout;
    private Integer requestTimeout;

    @NotNull
    private DataSize uploadSizeLimit;

    @Pattern(regexp = "^[a-zA-Z0-9-_]{0,25}$")
    private String messageChannel;

    private Set<String> reportees = Sets.newHashSet();

    @NotNull
    private Integer defaultTtlHours;

}
