package no.difi.meldingsutveksling.nextmove.validation;

import com.google.common.collect.Maps;
import no.difi.meldingsutveksling.ServiceIdentifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ConversationValidatorFactory {

    private Map<ServiceIdentifier, ConversationValidator> validators;

    @Autowired
    public ConversationValidatorFactory(ObjectProvider<List<ConversationValidator>> validatorList) {
        validators = Maps.newEnumMap(ServiceIdentifier.class);
        validatorList.getIfAvailable().forEach(v -> validators.put(v.getServicIdentifier(), v));
    }

    public Optional<ConversationValidator> getValidator(ServiceIdentifier si) {
        if (validators.containsKey(si)) {
            return Optional.of(validators.get(si));
        }
        return Optional.empty();
    }
}
