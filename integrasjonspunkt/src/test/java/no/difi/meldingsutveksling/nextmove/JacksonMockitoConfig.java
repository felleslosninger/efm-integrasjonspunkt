package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class JacksonMockitoConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonMockitoCustomizer() {

        return builder ->
                builder.annotationIntrospector(new JacksonAnnotationIntrospector() {

                    @Override
                    public boolean hasIgnoreMarker(final AnnotatedMember m) {
                        return super.hasIgnoreMarker(m) || m.getName().contains("Mockito");
                    }
                });
    }
}
