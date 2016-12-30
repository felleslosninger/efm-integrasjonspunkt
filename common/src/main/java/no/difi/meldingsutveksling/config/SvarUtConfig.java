package no.difi.meldingsutveksling.config;

import lombok.Data;
import no.difi.meldingsutveksling.CertificateParser;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Data
public class SvarUtConfig {
    private boolean kryptert = true;

    @Valid()
    @Pattern(regexp = "^[a-zA-Z0-9\\-\\.øæåØÆÅ]{0,20}$")
    private String konverteringsKode;
    private Resource certificatePath;


    public X509Certificate getCertificate() {
        try {
            return new CertificateParser().parse(new InputStreamReader(certificatePath.getInputStream()));
        } catch (IOException | CertificateException e) {
            throw new ConfigException("Unable to open certificate file", e);
        }
    }
}
