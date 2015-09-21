package no.difi.meldingsutveksling.config;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

/**
 * This file will read custom properties from integrasjonspunkt-local.properties
 * and put them into the Spring Application Context as if they were specified
 * in the application.properties file or as system properties. Only the properties
 * you specify below are added (see comment in code).
 *
 * Property initialization such as this class give us great freedom when doing custom
 * configuration in for example linked Docker-containers.
 *
 * @author Dervis M, 21/09/15.
 */
public class IntegrasjonspunktCustomPropertyListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktCustomPropertyListener.class);
    private static final String KEY_SERVICEURL = "spring.boot.admin.client.serviceUrl";
    private static final String PROPERTIES_FILE_NAME_OVERRIDE = "integrasjonspunkt-local.properties";

    private CompositeConfiguration config;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {

        // this class is called several times, this makes sure it runs only once
        if (config == null) {
            config = new CompositeConfiguration();
            try {
                PropertiesConfiguration configurationFileOverride = new PropertiesConfiguration(PROPERTIES_FILE_NAME_OVERRIDE);
                config.addConfiguration(configurationFileOverride);
            } catch (ConfigurationException e) {
                log.error("Coulndt not initialize properties: ", e);
            }

            ConfigurableEnvironment environment = event.getEnvironment();
            Properties props = new Properties();

            // Add the custom properties (only the ones you specify here) into the application context

            if (config.getString(KEY_SERVICEURL) != null) {
                props.put(KEY_SERVICEURL, config.getString(KEY_SERVICEURL));
                environment.getPropertySources().addFirst(new PropertiesPropertySource("docker", props));
            }
        }


    }

}
