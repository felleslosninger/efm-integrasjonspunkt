package no.difi.meldingsutveksling.nextmove.v2;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.PrototypeNotFoundException;
import no.difi.meldingsutveksling.validation.EnabledService;
import no.difi.meldingsutveksling.validation.ReceiverAcceptableServiceIdentifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

@RestController
@Api
@RequiredArgsConstructor
@RequestMapping("/api/prototype")
@Validated
public class PrototypeController {

    @GetMapping("{capabilityId}")
    @ApiOperation(value = "Get prototype", notes = "Get a prototype for a given capabilityId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = String.class)
    })
    public String getPrototype(@ApiParam(value = "capabilityId", required = true)
                               @PathVariable
                               @NotNull
                               @EnabledService
                               @ReceiverAcceptableServiceIdentifier
                                       String capabilityId) {
        // TODO: Implement method
        throw new PrototypeNotFoundException(capabilityId);
    }
}
