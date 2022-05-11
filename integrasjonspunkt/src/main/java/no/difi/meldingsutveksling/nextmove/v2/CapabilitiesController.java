package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.capabilities.Capabilities;
import no.difi.meldingsutveksling.exceptions.IdentifierNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.NotFoundInServiceRegistryException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/capabilities")
@Validated
public class CapabilitiesController {

    private final CapabilitiesFactory capabilitiesFactory;

    @GetMapping("{receiverIdentifier}")
    @Transactional
    @ResponseBody
    public Capabilities capabilities(
            @PathVariable @NotNull String receiverIdentifier,
            @RequestParam(required = false) Integer securityLevel,
            @RequestParam(required = false) String process) throws NotFoundInServiceRegistryException {
        try {
            return capabilitiesFactory.getCapabilities(receiverIdentifier, securityLevel, process);
        } catch (MeldingsUtvekslingRuntimeException e) {
            if (e.getCause() instanceof NotFoundInServiceRegistryException) {
                throw new IdentifierNotFoundException(e.getCause().getMessage());
            }
            throw e;
        }
    }

}
