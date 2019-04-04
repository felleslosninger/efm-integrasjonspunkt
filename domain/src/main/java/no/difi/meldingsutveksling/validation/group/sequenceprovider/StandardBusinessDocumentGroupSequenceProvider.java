package no.difi.meldingsutveksling.validation.group.sequenceprovider;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.validation.group.ValidationGroupFactory;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StandardBusinessDocumentGroupSequenceProvider implements DefaultGroupSequenceProvider<StandardBusinessDocument> {

    @Override
    public List<Class<?>> getValidationGroups(StandardBusinessDocument input) {
        List<Class<?>> defaultGroupSequence = new ArrayList<>();
        defaultGroupSequence.add(StandardBusinessDocument.class);

        getType(input).ifPresent(type ->
                ServiceIdentifier.safeValueOf(type)
                        .map(ValidationGroupFactory::toServiceIdentifier)
                        .filter(Objects::nonNull)
                        .ifPresent(defaultGroupSequence::add)
        );

        return defaultGroupSequence;
    }

    private Optional<String> getType(StandardBusinessDocument input) {
        return Optional.ofNullable(input)
                .map(StandardBusinessDocument::getStandardBusinessDocumentHeader)
                .map(StandardBusinessDocumentHeader::getDocumentIdentification)
                .map(DocumentIdentification::getType);
    }
}