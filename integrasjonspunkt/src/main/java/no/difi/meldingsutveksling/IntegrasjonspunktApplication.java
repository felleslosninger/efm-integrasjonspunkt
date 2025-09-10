package no.difi.meldingsutveksling;

import jakarta.annotation.PostConstruct;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.IntegrasjonspunktPropertiesValidator;
import no.difi.meldingsutveksling.config.VaultProtocolResolver;
import no.difi.meldingsutveksling.spring.IntegrasjonspunktLocalPropertyEnvironmentPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

import java.io.IOException;
import java.util.TimeZone;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_TIME_ZONE;

@SpringBootApplication
public class IntegrasjonspunktApplication extends SpringBootServletInitializer {

    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktApplication.class);

    @Bean
    public static Validator configurationPropertiesValidator() {
        return new IntegrasjonspunktPropertiesValidator();
    }

    @PostConstruct
    void started() {
        TimeZone.setDefault(DEFAULT_TIME_ZONE);
    }

    public static void main(String[] args) {

        ConfigurableApplicationContext context = new SpringApplicationBuilder(IntegrasjonspunktApplication.class)
                .initializers(new VaultProtocolResolver())
                .listeners(new IntegrasjonspunktLocalPropertyEnvironmentPostProcessor())
                .run(args);
        checkNtpSync(context);

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
                String errorStr = "Startup failed. Offset from NTP host %s was more than 5 seconds (%sms). Adjust local clock and try again.".formatted(host, offset);
                log.error(errorStr);
                String stars = "\n**************************\n";
                System.out.println(stars + errorStr + stars);
                context.close();
            }
        } catch (IOException e) {
            log.error("Error while syncing with NTP %s, continuing startup..".formatted(host), e);
        }
    }

}
