package no.difi.meldingsutveksling.nextmove.v2;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.capabilities.Capability;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.validation.InServiceRegistry;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Api
@RequiredArgsConstructor
@RequestMapping("/api/capabilities")
@Validated
public class CapabilitiesController {

    private final ServiceRegistryLookup sr;
    private final CapabilityFactory capabilityFactory;

    @GetMapping("{receiverid}")
    @ApiOperation(value = "Get all capabilities", notes = "Gets a list of all capabilities")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = String[].class),
            @ApiResponse(code = 400, message = "BadRequest", response = String.class)
    })
    public List<Capability> capabilities(@ApiParam(value = "receiverid", required = true)
                                         @PathVariable
                                         @NotNull
                                         @InServiceRegistry String receiverid) {
        return sr.getServiceRecords(receiverid)
                .stream()
                .map(capabilityFactory::getCapability)
                .collect(Collectors.toList());
    }
}
