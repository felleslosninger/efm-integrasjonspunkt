package no.difi.meldingsutveksling.config;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.BusinessMessageUtil;

import java.util.List;
import java.util.stream.Collectors;

public class BusinessMessageAliasIntrospector extends JacksonAnnotationIntrospector {

    private final List<PropertyName> businessMessageTypes;

    public BusinessMessageAliasIntrospector() {
        businessMessageTypes = BusinessMessageUtil.getMessageTypes().stream()
            .map(PropertyName::new)
            .collect(Collectors.toList());
    }

    @Override
    public List<PropertyName> findPropertyAliases(Annotated m) {
        if (m.getRawType() == StandardBusinessDocument.class && (
            m.getName().equals("getAny") || m.getName().equals("setAny"))) {
            return businessMessageTypes;
        }
        return super.findPropertyAliases(m);
    }
}
