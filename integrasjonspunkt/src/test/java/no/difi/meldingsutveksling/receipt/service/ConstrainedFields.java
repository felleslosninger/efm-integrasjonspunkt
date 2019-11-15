package no.difi.meldingsutveksling.receipt.service;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.util.StringUtils;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.snippet.Attributes.key;

class ConstrainedFields {

    private final ExtendedConstraintDescriptions constraintDescriptions;
    private final String prefix;
    private Class<?> group;

    ConstrainedFields(Class<?> input, String prefix) {
        this(input, prefix, null);
    }

    ConstrainedFields(Class<?> input, String prefix, Class<?> group) {
        this.constraintDescriptions = new ExtendedConstraintDescriptions(input);
        this.prefix = prefix;
        this.group = group;
    }

    FieldDescriptor withPath(String path) {
        return fieldWithPath(prefix + path).attributes(key("constraints").value(StringUtils
                .collectionToDelimitedString(this.group != null
                        ? this.constraintDescriptions.descriptionsForProperty(path, group)
                        : this.constraintDescriptions.descriptionsForProperty(path), ".\n")));
    }
}
