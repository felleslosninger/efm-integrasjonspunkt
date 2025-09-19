package no.difi.meldingsutveksling.config;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class Jwk {

    private Resource path; // peker på en fil med jwk (json web key)

}
