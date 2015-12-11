package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRequiredPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang.StringUtils.isBlank;

@Service
@PropertySources(value = {
        @PropertySource("classpath:properties/application-${spring.profiles.active}.properties"),
        @PropertySource("classpath:properties/integrasjonspunkt-${spring.profiles.active}.properties"),
        @PropertySource("classpath:integrasjonspunkt-local.properties")
})
public class IntegrasjonspunktConfiguration {
    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktConfiguration.class);

    private static final String KEY_ORGANISATION_NUMBER = "orgnumber";
    private static final String KEY_ALTINN_USERNAME = "altinn.username";
    private static final String KEY_ALTINN_PASSWORD = "altinn.password";
    private static final String KEY_ALTINN_SERVICE_CODE = "altinn.external_service_code";
    private static final String KEY_ALTINN_SERVICE_EDITION_CODE = "altinn.external_service_edition_code";

    private Environment environment;
    private String altinnServiceEditionCode;

    @Autowired
    public IntegrasjonspunktConfiguration(Environment environment) throws MeldingsUtvekslingRequiredPropertyException {
        this.environment = environment;
    }

    public String getProfile() {
        return this.environment.getProperty("spring.profiles.active");
    }

    public String getOrganisationNumber() {
        return environment.getProperty(KEY_ORGANISATION_NUMBER);
    }

    public boolean isQueueEnabled() {
        return Boolean.valueOf(environment.getProperty("toggle.enable.queue"));
    }

    public boolean hasOrganisationNumber() {
        String orgNumber = environment.getProperty(KEY_ORGANISATION_NUMBER);
        return orgNumber != null && !orgNumber.isEmpty();
    }

    private void validateProperty(String key) throws MeldingsUtvekslingRequiredPropertyException {
        if (isBlank(environment.getProperty(key))) {
            String message = String.format("Required property %s is missing. Check if parameter for key is set, either in integrasjonspunkt-local.properties or set as in-parameter on startup.", key);
            log.error(message);
            throw new MeldingsUtvekslingRequiredPropertyException(message);
        }
    }

    public String getAltinnUsername() {
        return environment.getProperty(KEY_ALTINN_USERNAME);
    }

    public String getAltinnPassword() {
        return environment.getProperty(KEY_ALTINN_PASSWORD);
    }

    public String getAltinnServiceCode() {
        return environment.getProperty(KEY_ALTINN_SERVICE_CODE);
    }

    public String getAltinnServiceEditionCode() {
        return environment.getProperty(KEY_ALTINN_SERVICE_EDITION_CODE);
    }
}
