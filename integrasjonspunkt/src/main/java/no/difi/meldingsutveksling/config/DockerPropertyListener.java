package no.difi.meldingsutveksling.config;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

/**
 * @author Dervis M, 21/09/15.
 */
public class DockerPropertyListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    static final String KEY_SERVICEURL = "spring.boot.admin.client.serviceUrl";
    private static final String PROPERTIES_FILE_NAME_OVERRIDE = "integrasjonspunkt-local.properties";

    private CompositeConfiguration config;

    public DockerPropertyListener() {

    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {

        if (config == null) {
            config = new CompositeConfiguration();
            config.addConfiguration(new SystemConfiguration());
            try {
                PropertiesConfiguration configurationFileOverride = new PropertiesConfiguration(PROPERTIES_FILE_NAME_OVERRIDE);
                config.addConfiguration(configurationFileOverride);
            } catch (ConfigurationException e) {
                System.out.println(e);
                System.exit(1);
            }

            if (config.getString(KEY_SERVICEURL) != null) {
                ConfigurableEnvironment environment = event.getEnvironment();
                Properties props = new Properties();
                props.put(KEY_SERVICEURL, config.getString(KEY_SERVICEURL));
                environment.getPropertySources().addFirst(new PropertiesPropertySource("docker", props));
            }
        }


    }

}
