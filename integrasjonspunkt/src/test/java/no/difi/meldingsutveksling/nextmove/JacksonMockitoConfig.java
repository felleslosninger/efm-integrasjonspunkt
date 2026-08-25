package no.difi.meldingsutveksling.nextmove;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

@Configuration
public class JacksonMockitoConfig {

    @Bean
    public JsonMapperBuilderCustomizer jacksonMockitoCustomizer() {

        return builder ->
                builder.annotationIntrospector(new JacksonAnnotationIntrospector() {

                    @Override
                    public boolean hasIgnoreMarker(MapperConfig<?> config, final AnnotatedMember m) {
                        return super.hasIgnoreMarker(config, m) || m.getName().contains("Mockito");
                    }
                });
    }
}
