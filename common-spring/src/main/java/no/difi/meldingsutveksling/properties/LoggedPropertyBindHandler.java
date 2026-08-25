package no.difi.meldingsutveksling.properties;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
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
        // Jackson 3: ObjectMapper er immutable, konfigurasjon skjer via builder
        om = JsonMapper.builder()
                .annotationIntrospector(new JacksonAnnotationIntrospector() {
                    @Override
                    public boolean hasIgnoreMarker(MapperConfig<?> config, AnnotatedMember m) {
                        return super.hasIgnoreMarker(config, m) ||
                            m.getName().toLowerCase().contains("token") ||
                            m.getName().toLowerCase().contains("password");
                    }
                })
                .build();
    }

    @Override
    public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) throws Exception {
        if (target.getAnnotation(LoggedProperty.class) != null) {
            log.info("Property set: %s = %s".formatted(name, om.writeValueAsString(result)));
        }

        super.onFinish(name, target, context, result);
    }

}
