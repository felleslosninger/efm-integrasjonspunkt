package no.difi.meldingsutveksling.config;

import com.google.common.io.Files;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * This file will read custom properties from integrasjonspunkt-local.properties
 * and put them into the Spring Application Context as if they were specified
 * in the application.properties file or as system properties. Only the properties
 * you specify below are added (see comment in code).
 *
 * Note: If a property exists as a system environment variable, then that one is used.
 *
 * Property initialization such as this class give us great freedom when doing custom
 * configuration in for example linked Docker-containers.
 *
 * @author Dervis M, 21/09/15.
 */
public class IntegrasjonspunktCustomPropertyListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktCustomPropertyListener.class);
    static final String KEY_SERVICEURL = "spring.boot.admin.client.serviceUrl";
    static final String KEY_SERVERURL = "spring.boot.admin.url";
    static final String KEY_CLIENTNAME = "spring.boot.admin.client.name";
    static final String KEY_DEREGISTRATION = "spring.boot.admin.autoDeregistration";

    // NB: autowiring does not work so good at the early stage when this listener is called,
    // so will not use the existing IntegrasjonspunktConfig class yet.
    private static final String PROPERTIES_FILE_NAME_OVERRIDE = "integrasjonspunkt-local.properties";
    private CompositeConfiguration config;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        // File may not exist in test context
        if (!new File(PROPERTIES_FILE_NAME_OVERRIDE).exists()) {
            return;
        }
        // Add the these custom properties (only the ones you specify here) into the application context
        List<String> list = Arrays.asList(
                KEY_SERVICEURL,
                KEY_SERVERURL,
                KEY_CLIENTNAME,
                KEY_DEREGISTRATION);

        // this class is called several times, this makes sure it runs only once
        if (config == null) {
            config = new CompositeConfiguration();
            try {
                PropertiesConfiguration configurationFileOverride = new PropertiesConfiguration(PROPERTIES_FILE_NAME_OVERRIDE);
                config.addConfiguration(configurationFileOverride);
            }
            catch (ConfigurationException e) {
                log.warn("Could not initialize properties: ", e);
            }

            ConfigurableEnvironment environment = event.getEnvironment();
            Properties props = new Properties();

            for (String customProperty : list) {
                String systemValue = System.getenv(customProperty);
                if (systemValue != null) {
                    props.put(customProperty, systemValue);
                    log.info(String.format("Added custom property from system environment: %s=%s", customProperty, systemValue));
                }
                else  {
                    String configValue = config.getString(customProperty);
                    if (configValue != null) {
                        props.put(customProperty, configValue);
                        log.info(String.format("Added custom property from local properties: %s=%s", customProperty, configValue));
                    }
                    else {
                        log.warn("Property " + customProperty + " was not found.");
                    }
                }
            }

            if (!props.isEmpty()) {
                environment.getPropertySources().addFirst(new PropertiesPropertySource("props", props));
            }
        }

    }

}
