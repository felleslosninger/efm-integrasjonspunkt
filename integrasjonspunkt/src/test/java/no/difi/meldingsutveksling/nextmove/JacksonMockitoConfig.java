package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class JacksonMockitoConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonMockitoCustomizer() {

        return builder ->
                builder.annotationIntrospector(new JacksonAnnotationIntrospector() {

                    @Override
                    public List<PropertyName> findPropertyAliases(Annotated m) {
                        if (m.getRawType() == StandardBusinessDocument.class && (
                            m.getName().equals("getAny") || m.getName().equals("setAny"))) {
                            return BusinessMessageUtil.getMessageTypes().stream()
                                .map(PropertyName::new)
                                .collect(Collectors.toList());
                        }
                        return super.findPropertyAliases(m);
                    }

                    @Override
                    public boolean hasIgnoreMarker(final AnnotatedMember m) {
                        return super.hasIgnoreMarker(m) || m.getName().contains("Mockito");
                    }
                });
    }
}
