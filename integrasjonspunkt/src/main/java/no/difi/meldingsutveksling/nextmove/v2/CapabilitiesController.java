package no.difi.meldingsutveksling.nextmove.v2;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Mod11Check;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Digits;
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

    @GetMapping("{receiverid}")
    @ApiOperation(value = "Get all capabilities", notes = "Gets a list of all capabilities")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = String[].class)
    })
    public List<String> capabilities(@ApiParam(value = "receiverid", required = true)
                                     @PathVariable
                                     @NotNull
                                     @Digits(integer = 9, fraction = 0)
                                     @Length(min = 9, max = 9)
                                     @Mod11Check(threshold = 7) String receiverid) {
        return sr.getServiceRecords(receiverid)
                .stream()
                .map(ServiceRecord::getServiceIdentifier)
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
