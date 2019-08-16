package no.difi.meldingsutveksling.validation.group.sequenceprovider;

import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.validation.group.ValidationGroupFactory;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PartnerIdentificationGroupSequenceProvider implements DefaultGroupSequenceProvider<PartnerIdentification> {

    @Override
    public List<Class<?>> getValidationGroups(PartnerIdentification input) {
        List<Class<?>> defaultGroupSequence = new ArrayList<>();
        defaultGroupSequence.add(PartnerIdentification.class);
        if (input != null) {
            Optional.ofNullable(ValidationGroupFactory.toPartner(input.getPartner())).ifPresent(defaultGroupSequence::add);
        }

        return defaultGroupSequence;
    }
}