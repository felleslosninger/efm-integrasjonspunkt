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
 * and put them in to the Spring Application Context as if they were specified
 * in application.properties or as a system property.
 *
 * Property initialization such as this class give us great freedom when doing custom
 * configuration in for example linked Docker-containers.
 *
 * @author Dervis M, 21/09/15.
 */
public class IntegrasjonspunktCustomPropertyListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    static final String KEY_SERVICEURL = "spring.boot.admin.client.serviceUrl";
    private static final String PROPERTIES_FILE_NAME_OVERRIDE = "integrasjonspunkt-local.properties";

    private CompositeConfiguration config;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {

        // this class is called several times, this makes sure it runs only once
        if (config == null) {
            config = new CompositeConfiguration();
            config.addConfiguration(new SystemConfiguration());
            try {
                PropertiesConfiguration configurationFileOverride = new PropertiesConfiguration(PROPERTIES_FILE_NAME_OVERRIDE);
                config.addConfiguration(configurationFileOverride);
            } catch (ConfigurationException e) {
                log.error("Coulndt not initialize properties: ", e);
            }

            ConfigurableEnvironment environment = event.getEnvironment();
            Properties props = new Properties();

            // Add the custom properties (only the once you specify here) into the application context

            if (config.getString(KEY_SERVICEURL) != null) {
                props.put(KEY_SERVICEURL, config.getString(KEY_SERVICEURL));
                environment.getPropertySources().addFirst(new PropertiesPropertySource("docker", props));
            }
        }


    }

}
