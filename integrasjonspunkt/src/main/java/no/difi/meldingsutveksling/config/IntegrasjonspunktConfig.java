package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.apache.commons.configuration.*;

/**
 * Class with responsibility of keeping track of configuration of the "integrasjonspunkt". The configruation
 * is loaded from two sources. application.proeprties, a file located in the working directory and System
 * properties that the VM is started with (-D options).
 * <p/>
 * <p>
 * If integrasjonspunkt-local.properties is not found in the working directory, the classpath will be searched.
 * <p/>
 * <p/>
 * The class is implemented as a singleton to avoid expensive loading properites on every instantiation
 * <p/>
 * <p>
 * Configuration properties for the integrasjonspunkt itself are given explicit methods in this class. Properties
 * for alternative transports are accessed through the exposed configuration object accessed via getConfiguration()
 * <p/>
 * </p>
 *
 * @author Glenn Bech
 */
public class IntegrasjonspunktConfig {

    private static final String PROPERTIES_FILE_NAME = "integrasjonspunkt.properties";
    private static final String PROPERTIES_FILE_NAME_OVERRIDE = "integrasjonspunkt-local.properties";

    static final String KEY_NOARKSYSTEM_ENDPOINT = "noarksystem.endpointURL";
    static final String KEY_PRIVATEKEYALIAS = "privatekeyalias";
    static final String KEY_KEYSTORE_LOCATION = "keystorelocation";
    static final String KEY_PRIVATEKEYPASSWORD = "privatekeypassword";
    static final String KEY_ADRESSEREGISTER_ENDPOINT = "adresseregister.endPointURL";

    private final CompositeConfiguration config;

    private static IntegrasjonspunktConfig instance;

    public static IntegrasjonspunktConfig getInstance() {
        if (instance == null) {
            instance = new IntegrasjonspunktConfig();
        }
        return instance;
    }

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
