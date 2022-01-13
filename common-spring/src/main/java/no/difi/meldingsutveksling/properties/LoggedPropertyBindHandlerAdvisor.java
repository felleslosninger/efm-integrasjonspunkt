package no.difi.meldingsutveksling.properties;

import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.stereotype.Component;

@Component
public class LoggedPropertyBindHandlerAdvisor implements ConfigurationPropertiesBindHandlerAdvisor {

    @Override
    public BindHandler apply(BindHandler bindHandler) {
        return new LoggedPropertyBindHandler(bindHandler);
    }

}
