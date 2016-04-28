package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRequiredPropertyException;
import no.difi.meldingsutveksling.noarkexchange.NoarkClientSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.commons.lang.StringUtils.isBlank;

@Component
@PropertySources(value = {
        @PropertySource("classpath:properties/application.properties"),
        @PropertySource("classpath:properties/integrasjonspunkt.properties"),
        @PropertySource("classpath:properties/application-${spring.profiles.active}.properties"),
        @PropertySource("classpath:properties/integrasjonspunkt-${spring.profiles.active}.properties"),
        @PropertySource("file:integrasjonspunkt-local.properties")
})
public class IntegrasjonspunktConfiguration {
    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktConfiguration.class);

    public static final String KEY_ORGANISATION_NUMBER = "orgnumber";
    private static final String KEY_ALTINN_USERNAME = "altinn.username";
    private static final String KEY_ALTINN_PASSWORD = "altinn.password";
    private static final String KEY_ALTINN_SERVICE_CODE = "altinn.external_service_code";
    private static final String KEY_ALTINN_SERVICE_EDITION_CODE = "altinn.external_service_edition_code";

    protected static final String KEY_NOARKSYSTEM_ENDPOINT = "noarksystem.endpointURL";
    private static final String KEY_NOARKSYSTEM_USERNAME = "noarksystem.username";
    private static final String KEY_NOARKSYSTEM_PASSWORD = "noarksystem.password";
    private static final String KEY_NOARKSYSTEM_DOMAIN = "noarksystem.domain";

    private static final String KEY_MSH_ENDPOINT = "msh.endpointURL";
    private static final String KEY_MSH_USERNAME = "msh.userName";
    private static final String KEY_MSH_PASSWORD = "msh.password";

    protected static final String KEY_ADRESSEREGISTER_ENDPOINT = "adresseregister.endPointURL";


    protected static final String KEY_PRIVATEKEYALIAS = "privatekeyalias";
    protected static final String KEY_KEYSTORE_LOCATION = "keystorelocation";
    protected static final String KEY_PRIVATEKEYPASSWORD = "privatekeypassword";
    protected static final String KEY_NOARKSYSTEM_TYPE = "noarksystem.type";

    private static final String KEY_SERVICEURL = "spring.boot.admin.client.serviceUrl";
    private static final String KEY_SERVERURL = "spring.boot.admin.url";
    private static final String KEY_CLIENTNAME = "spring.boot.admin.client.name";
    private static final String KEY_DEREGISTRATION = "spring.boot.admin.autoDeregistration";

    private static final String KEY_RETURN_OK_ONMISSINGPAYLOAD = "Return.Ok.OnEmptyPayload";

    private Environment environment;

    @Autowired
    public IntegrasjonspunktConfiguration(Environment environment) throws MeldingsUtvekslingRequiredPropertyException {
        this.environment = new WhiteSpaceTrimmingEnvironmentDecorator(environment);

        validateProperty(KEY_NOARKSYSTEM_ENDPOINT);
        validateProperty(KEY_ADRESSEREGISTER_ENDPOINT);
        validateProperty(KEY_PRIVATEKEYALIAS);
        validateProperty(KEY_KEYSTORE_LOCATION);
        validateProperty(KEY_PRIVATEKEYPASSWORD);
        validateProperty(KEY_ORGANISATION_NUMBER);
        validateProperty(KEY_NOARKSYSTEM_TYPE);
        MDC.put(IntegrasjonspunktConfiguration.KEY_ORGANISATION_NUMBER, getOrganisationNumber());
        validateSpringMetrics();
    }

    public String getProfile() {
        return this.environment.getProperty("spring.profiles.active");
    }

    public boolean isQueueEnabled() {
        return Boolean.valueOf(environment.getProperty("toggle.enable.queue"));
    }

    public boolean hasOrganisationNumber() {
        String orgNumber = environment.getProperty(KEY_ORGANISATION_NUMBER);
        return orgNumber != null && !orgNumber.isEmpty();
    }

    public NoarkClientSettings getLocalNoarkClientSettings() {
        return new NoarkClientSettings(getNOARKSystemEndPointURL(), getNoarksystemUsername(), getKeyNoarksystemPassword(), getNoarksystemDomain());
    }

    public NoarkClientSettings getMshNoarkClientSettings() {
        return new NoarkClientSettings(getNOARKSystemEndPointURL(), getNoarksystemUsername(), getKeyNoarksystemPassword(), getNoarksystemDomain());
    }

    public Environment getConfiguration() {
        return environment;
    }

    public String getOrganisationNumber() {
        return environment.getProperty(KEY_ORGANISATION_NUMBER);
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

    public String getAdresseRegisterEndPointURL() {
        return environment.getProperty(KEY_ADRESSEREGISTER_ENDPOINT);
    }

    public String getKeyStoreLocation() {
        return environment.getProperty(KEY_KEYSTORE_LOCATION);
    }

    public String getPrivateKeyPassword() {
        return environment.getProperty(KEY_PRIVATEKEYPASSWORD);
    }

    public String getPrivateKeyAlias() {
        return environment.getProperty(KEY_PRIVATEKEYALIAS);
    }

    public String getNoarkType() {
        return environment.getProperty(KEY_NOARKSYSTEM_TYPE);
    }

    public String getKeyMshEndpoint() {return environment.getProperty(KEY_MSH_ENDPOINT); }

    public boolean getReturnOkOnMissingPayload() {
        return Boolean.valueOf(environment.getProperty(KEY_RETURN_OK_ONMISSINGPAYLOAD));
    }

    public ConfigMeta getMetadata() {

        ConfigMeta.Builder b = new ConfigMeta.Builder();
        return b.newElement(KEY_ORGANISATION_NUMBER, getOrganisationNumber())
                .newGroup("Altinn")
                .newElement(KEY_ALTINN_USERNAME, getAltinnUsername())
                .newElement(KEY_ALTINN_SERVICE_CODE, getAltinnServiceCode())
                .newElement(KEY_ALTINN_SERVICE_EDITION_CODE, getAltinnServiceEditionCode())
                .newGroup("Keystore")
                .newElement(KEY_KEYSTORE_LOCATION, getCurrentPath() + File.separator + getKeyStoreLocation())
                .newElement(KEY_PRIVATEKEYALIAS, getPrivateKeyAlias())
                .newGroup("Adresseregister")
                .newElement(KEY_ADRESSEREGISTER_ENDPOINT, getAdresseRegisterEndPointURL())
                .newGroup("Noark")
                .newElement(KEY_NOARKSYSTEM_TYPE, getNoarkType())
                .newElement(KEY_NOARKSYSTEM_ENDPOINT, getNOARKSystemEndPointURL())
                .newElement(KEY_NOARKSYSTEM_DOMAIN, getNoarksystemDomain())
                .newElement(KEY_NOARKSYSTEM_USERNAME, getNoarksystemUsername())
                .newElement(KEY_NOARKSYSTEM_PASSWORD)
                .newGroup("Msh")
                .newElement(KEY_MSH_ENDPOINT, getMshNoarkClientSettings().getEndpointUrl())
                .newElement(KEY_MSH_USERNAME, getMshNoarkClientSettings().getUserName())
                .newElement(KEY_MSH_PASSWORD)
                .newGroup("Misc")
                .newElement(KEY_RETURN_OK_ONMISSINGPAYLOAD, environment.getProperty(KEY_RETURN_OK_ONMISSINGPAYLOAD))
                .build();
    }

    private String getCurrentPath() {
        Path r = Paths.get("");
        return r.toAbsolutePath().toString();
    }

    private void validateProperty(String key) throws MeldingsUtvekslingRequiredPropertyException {
        if (isBlank(environment.getProperty(key))) {
            String message = String.format("Required property %s is missing. Check if parameter for key is set, either in integrasjonspunkt-local.properties or set as in-parameter on startup.", key);
            log.error(message);
            throw new MeldingsUtvekslingRequiredPropertyException(message);
        }
    }

    private void validateSpringMetrics() {
        if (isBlank(environment.getProperty(KEY_SERVERURL))
                || isBlank(environment.getProperty(KEY_SERVICEURL))
                || isBlank(environment.getProperty(KEY_CLIENTNAME))
                || isBlank(environment.getProperty(KEY_DEREGISTRATION))) {
            log.warn("Not all parameters for metrics are set. Metrics might not work properly.");
        }
    }

    private String getNOARKSystemEndPointURL() {
        return environment.getProperty(KEY_NOARKSYSTEM_ENDPOINT);
    }

    private String getNoarksystemUsername() {
        return environment.getProperty(KEY_NOARKSYSTEM_USERNAME);
    }

    private String getKeyNoarksystemPassword() {
        return environment.getProperty(KEY_NOARKSYSTEM_PASSWORD);
    }

    private String getNoarksystemDomain() {
        return environment.getProperty(KEY_NOARKSYSTEM_DOMAIN);
    }


}
