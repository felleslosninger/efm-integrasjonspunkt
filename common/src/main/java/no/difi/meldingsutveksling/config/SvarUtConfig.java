package no.difi.meldingsutveksling.config;

import lombok.Data;
import no.difi.meldingsutveksling.CertificateParser;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Data
public class SvarUtConfig {
    private boolean kryptert = true;

    @Valid()
    @Pattern(regexp = "^[a-zA-Z0-9\\-\\.øæåØÆÅ]{0,20}$")
    private String konverteringsKode;

    @NotNull
    private Resource certificatePath;

    private String username;

    private String password;


    public X509Certificate getCertificate() {
        final InputStream inputStream;
        try {
            inputStream = certificatePath.getInputStream();
        } catch (IOException e) {
            throw new ConfigException("Unable to open inputstream for certificate", e);
        }

        try {
            return new CertificateParser().parse(new InputStreamReader(inputStream));
        } catch (IOException | CertificateException e) {
            throw new ConfigException("Unable to open certificate file", e);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
