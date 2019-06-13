package no.difi.meldingsutveksling.config;

import lombok.Data;
import lombok.ToString;

/**
 *
 * @author Nikolai Luthman <nikolai dot luthman at inmeta dot no>
 */
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
    private Integer connectTimeout;
    private Integer requestTimeout;

}
