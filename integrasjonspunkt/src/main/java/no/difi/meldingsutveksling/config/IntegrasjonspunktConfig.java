package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRequiredPropertyException;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.NoarkClientSettings;
import org.apache.commons.configuration.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Class with responsibility of keeping track of configuration of the "integrasjonspunkt". The configruation
 * is loaded from three sources.
 * <p>
 * When a property is loaded, it is searched for in the following order;
 * <ol>
 * <li> System property suplied when the VM starts up (-D option on the command line) </li>
 * <li> integrasjonspunkt-local.properties, usualy a file placed in the working directory. Use this
 * to override properties for you local dev. environment</li>
 * <li> integrasjonspunkt.properties, a file packaged inside the application (from src/main/resources/) This provides
 * default values and contain end point and properties for the test-environment </li>
 * </li>
 * </ol>
 *
 * @author Glenn Bech
 */
@Component
public class IntegrasjonspunktConfig {
    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktConfig.class);

    private static final String PROPERTIES_FILE_NAME = "integrasjonspunkt.properties";
    private static final String PROPERTIES_FILE_NAME_OVERRIDE = "integrasjonspunkt-local.properties";

    static final String KEY_NOARKSYSTEM_ENDPOINT = "noarksystem.endpointURL";
    static final String KEY_NOARKSYSTEM_USERNAME = "noarksystem.userName";
    static final String KEY_NOARKSYSTEM_PASSWORD = "noarksystem.password";
    static final String KEY_NOARKSYSTEM_DOMAIN = "noarksystem.domain";

    static final String KEY_MSH_ENDPOINT = "msh.endpointURL";
    static final String KEY_MSH_USERNAME = "msh.userName";
    static final String KEY_MSH_PASSWORD = "msh.password";

    static final String KEY_ADRESSEREGISTER_ENDPOINT = "adresseregister.endPointURL";
    static final String KEY_ADRESSEREGISTER_USERNAME = "adresseregister.userName";
    static final String KEY_ADRESSEREGISTER_PASSWORD = "adresseregister.password";

    static final String KEY_PRIVATEKEYALIAS = "privatekeyalias";
    static final String KEY_KEYSTORE_LOCATION = "keystorelocation";
    static final String KEY_PRIVATEKEYPASSWORD = "privatekeypassword";
    private static final String KEY_ORGANISATION_NUMBER = "orgnumber";
    public static final String NOARKSYSTEM_TYPE = "noarksystem.type";

    private final CompositeConfiguration config;

    private IntegrasjonspunktConfig() throws MeldingsUtvekslingRequiredPropertyException {

        config = new CompositeConfiguration();
        config.addConfiguration(new SystemConfiguration());
        try {
            PropertiesConfiguration configurationFileOverride = new PropertiesConfiguration(PROPERTIES_FILE_NAME_OVERRIDE);
            config.addConfiguration(configurationFileOverride);
        } catch (ConfigurationException e) {
            // okey to not have local config
        }

        try {
            PropertiesConfiguration configurationFile = new PropertiesConfiguration(PROPERTIES_FILE_NAME);
            config.addConfiguration(configurationFile);
        } catch (ConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException("The configuration file " + PROPERTIES_FILE_NAME + " not found on classpath.", e);
        }

        validateProperty(KEY_NOARKSYSTEM_ENDPOINT);
        validateProperty(KEY_MSH_ENDPOINT);
        validateProperty(KEY_ADRESSEREGISTER_ENDPOINT);
        validateProperty(KEY_PRIVATEKEYALIAS);
        validateProperty(KEY_KEYSTORE_LOCATION);
        validateProperty(KEY_PRIVATEKEYPASSWORD);
        validateProperty(NOARKSYSTEM_TYPE);
    }

    public String getAdresseRegisterEndPointURL() {
        return config.getString(KEY_ADRESSEREGISTER_ENDPOINT);
    }

    public String getKeyAdresseregisterUsername() {
        return config.getString(KEY_ADRESSEREGISTER_USERNAME);
    }

    public String getKeyAdresseregisterPassword() {
        return config.getString(KEY_ADRESSEREGISTER_PASSWORD);
    }

    private String getNOARKSystemEndPointURL() {
        return config.getString(KEY_NOARKSYSTEM_ENDPOINT);
    }

    private String getNoarksystemUsername() {
        return config.getString(KEY_NOARKSYSTEM_USERNAME);
    }

    private String getKeyNoarksystemPassword() {
        return config.getString(KEY_NOARKSYSTEM_PASSWORD);
    }

    private String getNoarksystemDomain() {
        return config.getString(KEY_NOARKSYSTEM_DOMAIN);
    }

    private String getMshEndpointUrl() {
        return config.getString(KEY_MSH_ENDPOINT);
    }

    private String getMshUsername() {
        return config.getString(KEY_MSH_USERNAME);
    }

    private String getMshPassword() {
        return config.getString(KEY_MSH_PASSWORD);
    }

    public String getKeyStoreLocation() {
        return config.getString(KEY_KEYSTORE_LOCATION);
    }

    public String getPrivateKeyPassword() {
        return config.getString(KEY_PRIVATEKEYPASSWORD);
    }

    public String getPrivateKeyAlias() {
        return config.getString(KEY_PRIVATEKEYALIAS);
    }

    public String getNoarkType() {
        return config.getString(NOARKSYSTEM_TYPE);
    }

    public Configuration getConfiguration() {
        return config;
    }

    public String getOrganisationNumber() {
        return config.getString(KEY_ORGANISATION_NUMBER);
    }

    public boolean hasOrganisationNumber() {
        String orgNumber = config.getString(KEY_ORGANISATION_NUMBER);
        return orgNumber != null && !orgNumber.isEmpty();
    }

    public NoarkClientSettings getLocalNoarkClientSettings() {
        return new NoarkClientSettings(getNOARKSystemEndPointURL(), getNoarksystemUsername(), getKeyNoarksystemPassword(), getNoarksystemDomain());
    }

    public NoarkClientSettings getMshNoarkClientSettings() {
        return new NoarkClientSettings(getNOARKSystemEndPointURL(), getNoarksystemUsername(), getKeyNoarksystemPassword(), getNoarksystemDomain());
    }

    private boolean validateProperty(String key) throws MeldingsUtvekslingRequiredPropertyException {
        if (isBlank(config.getString(key))) {
            String message = String.format("Required property %s is missing. Check if parameter for key is set, either in integrasjonspunkt-local.properties or set as in-parameter on startup.", key);
            log.error(message);
            throw new MeldingsUtvekslingRequiredPropertyException(message);
        }
        return true;
    }
}