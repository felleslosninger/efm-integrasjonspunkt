package no.difi.meldingsutveksling.validation.group.sequenceprovider;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;
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
                        .map(this::getValidationGroup)
                        .filter(Objects::nonNull)
                        .ifPresent(defaultGroupSequence::add)
        );

        return defaultGroupSequence;
    }

    private Class<?> getValidationGroup(ServiceIdentifier serviceIdentifier) {
        switch (serviceIdentifier) {
            case DPO:
                return ValidationGroups.ServiceIdentifier.Dpo.class;
            case DPV:
                return ValidationGroups.ServiceIdentifier.Dpv.class;
            case DPI_DIGITAL:
                return ValidationGroups.ServiceIdentifier.DpiDigital.class;
            case DPI_PRINT:
                return ValidationGroups.ServiceIdentifier.DpiPrint.class;
            case DPF:
                return ValidationGroups.ServiceIdentifier.Dpf.class;
            case DPE_INNSYN:
                return ValidationGroups.ServiceIdentifier.DpeInnsyn.class;
            case DPE_DATA:
                return ValidationGroups.ServiceIdentifier.DpeData.class;
            case DPE_RECEIPT:
                return ValidationGroups.ServiceIdentifier.DpeReceipt.class;
            default:
                return null;
        }
    }

    private Optional<String> getType(StandardBusinessDocument input) {
        return Optional.ofNullable(input)
                .map(StandardBusinessDocument::getStandardBusinessDocumentHeader)
                .map(StandardBusinessDocumentHeader::getDocumentIdentification)
                .map(DocumentIdentification::getType);
    }
}