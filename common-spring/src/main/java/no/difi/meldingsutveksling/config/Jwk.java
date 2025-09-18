package no.difi.meldingsutveksling.config;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class Jwk {

    private String kid; // kanskje vi kan utlede den fra JWK pakken?
    private Resource path; // peker på en fil med jwk (json web key)

}
