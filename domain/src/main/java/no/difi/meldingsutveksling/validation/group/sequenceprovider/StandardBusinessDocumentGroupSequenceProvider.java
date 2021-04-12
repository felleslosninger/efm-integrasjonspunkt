package no.difi.meldingsutveksling.validation.group.sequenceprovider;

import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.validation.group.ValidationGroupFactory;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StandardBusinessDocumentGroupSequenceProvider implements DefaultGroupSequenceProvider<StandardBusinessDocument> {

    @Override
    public List<Class<?>> getValidationGroups(StandardBusinessDocument input) {
        List<Class<?>> defaultGroupSequence = new ArrayList<>();
        defaultGroupSequence.add(StandardBusinessDocument.class);

        getType(input).flatMap(type -> MessageType.valueOfType(type).map(ValidationGroupFactory::toDocumentType))
                .ifPresent(defaultGroupSequence::add);

        return defaultGroupSequence;
    }

    private Optional<String> getType(StandardBusinessDocument input) {
        return Optional.ofNullable(input)
                .flatMap(p -> Optional.ofNullable(p.getStandardBusinessDocumentHeader()))
                .flatMap(p -> Optional.ofNullable(p.getDocumentIdentification()))
                .flatMap(p -> Optional.ofNullable(p.getType()));
    }
}