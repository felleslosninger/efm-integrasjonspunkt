package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.apache.commons.configuration.*;
import org.springframework.stereotype.Component;

/**
 * Class with responsibility of keeping track of configuration of the "integrasjonspunkt". The configruation
 * is loaded from three sources.
 * <p/>
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

    private static final String PROPERTIES_FILE_NAME = "integrasjonspunkt.properties";
    private static final String PROPERTIES_FILE_NAME_OVERRIDE = "integrasjonspunkt-local.properties";

    static final String KEY_NOARKSYSTEM_ENDPOINT = "noarksystem.endpointURL";
    static final String KEY_PRIVATEKEYALIAS = "privatekeyalias";
    static final String KEY_KEYSTORE_LOCATION = "keystorelocation";
    static final String KEY_PRIVATEKEYPASSWORD = "privatekeypassword";
    static final String KEY_ADRESSEREGISTER_ENDPOINT = "adresseregister.endPointURL";

    private final CompositeConfiguration config;

    private IntegrasjonspunktConfig() {

        config = new CompositeConfiguration();
        config.addConfiguration(new SystemConfiguration());

        try {
            PropertiesConfiguration configurationFileOverride = new PropertiesConfiguration(PROPERTIES_FILE_NAME_OVERRIDE);
            config.addConfiguration(configurationFileOverride);

            PropertiesConfiguration configurationFile = new PropertiesConfiguration(PROPERTIES_FILE_NAME);
            config.addConfiguration(configurationFile);

        } catch (ConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException("The configuration file application.properties not found in working directory.");
        }
    }


    public String getAdresseRegisterEndPointURL() {
        return config.getString(KEY_ADRESSEREGISTER_ENDPOINT);
    }

    public String getNOARKSystemEndPointURL() {
        return config.getString(KEY_NOARKSYSTEM_ENDPOINT);
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

    public Configuration getConfiguration() {
        return config;
    }
}
