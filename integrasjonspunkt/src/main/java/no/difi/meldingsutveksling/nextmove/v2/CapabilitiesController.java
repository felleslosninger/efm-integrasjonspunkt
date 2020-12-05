package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.capabilities.Capabilities;
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
            @RequestParam(required = false) Integer securityLevel) {
        return capabilitiesFactory.getCapabilities(receiverIdentifier, securityLevel);
    }
}
