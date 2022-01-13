package no.difi.meldingsutveksling.properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

@Slf4j
public class LoggedPropertyBindHandler extends AbstractBindHandler {

    private final ObjectMapper om;

    public LoggedPropertyBindHandler(BindHandler parent) {
        super(parent);
        om = new ObjectMapper();
        om.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            protected boolean _isIgnorable(Annotated a) {
                return a.getName().toLowerCase().contains("token") ||
                    a.getName().toLowerCase().contains("password");
            }
        });
    }

    @Override
    public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) throws Exception {
        if (target.getAnnotation(LoggedProperty.class) != null) {
            log.info(String.format("Property set: %s = %s", name, om.writeValueAsString(result)));
        }

        super.onFinish(name, target, context, result);
    }

}
