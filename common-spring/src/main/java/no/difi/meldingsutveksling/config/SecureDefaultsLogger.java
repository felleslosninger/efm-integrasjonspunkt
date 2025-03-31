package no.difi.meldingsutveksling.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecureDefaultsLogger {

    private final Environment env;

    public SecureDefaultsLogger(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void logIfInsecureDefaults() {
        warnAbout("difi.security.enable", "false",
            "[difi.security.enable] Det anbefales å konfigurere integrasjonspunktets API med brukernavn og passord. Merk at dette krever at system(ene) som skal integrere med integrasjonspunktet støtter dette.");
        warnAbout("difi.ssl.enabled", "false",
            "[difi.ssl.enabled] Det anbefales å konfigurere integrasjonspunktets API med TLS. Merk at dette krever at system(ene) som skal integrere med integrasjonspunktet støtter dette.");
        warnAbout("difi.move.org.keystore.type", "JKS",
            "[difi.move.org.keystore.type]. Det anbefales å oppbevare virksomhetssertifikat som PKCS12 (.p12) istedenfor Java Keystore (.jks).");
        warnAbout("difi.datasource.url", "jdbc:h2:file:./integrasjonspunkt",
            "[difi.datasource.url] Det anbefales å konfigurere integrasjonspunktet med ekstern database til fordel for den interne fildatabasen (H2).");
        warnAbout("difi.activemq.broker-url", "vm://localhost",
            "[difi.activemq.broker-url] Det anbefales å konfigurere integrasjonspunktet med ekstern meldingskø til fordel for den interne.");
    }

    private void warnAbout(String key, String unsafeValue, String message) {
        String value = env.getProperty(key);
        if (unsafeValue.equalsIgnoreCase(value)) {
            log.warn("{} (Gjeldende verdi: {})", message, value);
        }
    }
}
