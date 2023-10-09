package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.IntegrasjonspunktPropertiesValidator;
import no.difi.meldingsutveksling.config.VaultProtocolResolver;
import no.difi.meldingsutveksling.spring.IntegrasjonspunktLocalPropertyEnvironmentPostProcessor;
import no.difi.move.common.config.SpringCloudProtocolResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;
import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.TimeZone;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_TIME_ZONE;

@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class IntegrasjonspunktApplication extends SpringBootServletInitializer {

    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktApplication.class);
    private static final String MISSING_JCE_MESSAGE = "Failed startup. Possibly unlimited security policy files that is not updated."
            + "/r/nTo fix this, download and replace policy files for the appropriate java version (found in ${java.home}/jre/lib/security/)"
            + "/r/n- Java7: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html"
            + "/r/n- Java8: http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html";

    @Bean
    public static Validator configurationPropertiesValidator() {
        return new IntegrasjonspunktPropertiesValidator();
    }

    @PostConstruct
    void started() {
        TimeZone.setDefault(DEFAULT_TIME_ZONE);
    }

    public static void main(String[] args) {
        try {
            if (!validateJCE()) {
                logMissingJCE();
                return;
            }

            ConfigurableApplicationContext context = new SpringApplicationBuilder(IntegrasjonspunktApplication.class)
                    .initializers(new SpringCloudProtocolResolver(), new VaultProtocolResolver())
                    .listeners(new IntegrasjonspunktLocalPropertyEnvironmentPostProcessor())
                    .run(args);
            checkNtpSync(context);

        } catch (SecurityException se) {
            logMissingJCE(se);
        }
    }

    @SuppressWarnings("squid:S106")
    private static void checkNtpSync(ConfigurableApplicationContext context) {
        IntegrasjonspunktProperties props = context.getBean(IntegrasjonspunktProperties.class);
        if (props.getNtp().isDisable()) {
            log.debug("NTP Check disabled");
            return;
        }
        String host = props.getNtp().getHost();
        log.debug("Checking offset for NTP host {}", host);
        try {
            NTPClient client = new NTPClient(host);
            long offset = client.getOffset();
            if (Math.abs(offset) > 5000) {
                String errorStr = String.format("Startup failed. Offset from NTP host %s was more than 5 seconds (%sms). Adjust local clock and try again.", host, offset);
                log.error(errorStr);
                String stars = "\n**************************\n";
                System.out.println(stars + errorStr + stars);
                context.close();
            }
        } catch (IOException e) {
            log.error(String.format("Error while syncing with NTP %s, continuing startup..", host), e);
        }
    }

    @SuppressWarnings("squid:S106")
    private static void logMissingJCE(Exception e) {
        System.out.println(MISSING_JCE_MESSAGE);
        log.error(MISSING_JCE_MESSAGE);
        log.error(e.getMessage());
    }

    @SuppressWarnings("squid:S106")
    private static void logMissingJCE() {
        System.out.println(MISSING_JCE_MESSAGE);
        log.error(MISSING_JCE_MESSAGE);
    }

    private static boolean validateJCE() {
        try {
            int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
            return maxKeyLen > 128;
        } catch (NoSuchAlgorithmException ex) {
            return false;
        }
    }
}
