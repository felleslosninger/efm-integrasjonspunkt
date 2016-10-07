package no.difi.meldingsutveksling.spring;

import java.util.Properties;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 *
 * @author kons-nlu
 */
public class TrimPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    @Override
    protected String resolvePlaceholder(String placeholder, Properties props) {
        String value = super.resolvePlaceholder(placeholder, props);

        return (value != null ? value.trim() : null);
    }
}
