package no.difi.meldingsutveksling.nextmove;

import org.springframework.restdocs.payload.FieldDescriptor;

import java.util.*;

class FieldDescriptorsBuilder {

    private final List<FieldDescriptor> descriptors = new ArrayList<>();

    FieldDescriptorsBuilder fields(FieldDescriptor... in) {
        descriptors.addAll(Arrays.asList(in));
        return this;
    }

    FieldDescriptorsBuilder fields(Collection<FieldDescriptor> in) {
        descriptors.addAll(in);
        return this;
    }

    List<FieldDescriptor> build() {
        return Collections.unmodifiableList(descriptors);
    }
}
