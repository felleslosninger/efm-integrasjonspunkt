package no.difi.meldingsutveksling.nextmove.v2;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.capabilities.Capabilities;
import no.difi.meldingsutveksling.validation.InServiceRegistry;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

@RestController
@Api(tags = "Capabilities")
@RequiredArgsConstructor
@RequestMapping("/api/capabilities")
@Validated
public class CapabilitiesController {

    private final CapabilitiesFactory capabilitiesFactory;

    @GetMapping("{receiverid}")
    @ApiOperation(
            value = "Get all capabilities",
            notes = "Gets information of all capabilities for a given receiver",
            response = Capabilities.class)
    @ApiResponses({
            @ApiResponse(
                    code = 200,
                    message = "Success",
                    response = Capabilities.class),
            @ApiResponse(code = 400, message = "BadRequest", response = String.class)
    })
    @Transactional
    @ResponseBody
    public Capabilities capabilities(
            @ApiParam(value = "receiverid", required = true)
            @PathVariable
            @NotNull @InServiceRegistry String receiverid) {
        return capabilitiesFactory.getCapabilities(receiverid);
    }
}
