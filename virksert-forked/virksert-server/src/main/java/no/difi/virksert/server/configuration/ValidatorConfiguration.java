package no.difi.virksert.server.configuration;

import no.difi.certvalidator.Validator;
import no.difi.virksert.security.BusinessCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("unused")
@Configuration
public class ValidatorConfiguration {

    private static Logger logger = LoggerFactory.getLogger(ValidatorConfiguration.class);

    @Value("${scope}")
    private String scope;

    @Bean
    public Validator getValidator() {
        logger.debug("Scope: {}", scope);

        return BusinessCertificate.getValidator(scope);
    }

}
