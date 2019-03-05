package no.difi.meldingsutveksling.nextmove.v2;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import javax.validation.Valid;

@RestController
@Validated
@Api
@RequestMapping("/api/message/out")
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageOutController {

    private final StandardBusinessDocumentRepository standardBusinessDocumentRepository;

    @PostMapping
    @ApiOperation(value = "Create conversation", notes = "Create a new conversation with the given values")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument createConversation(
            @Valid @RequestBody StandardBusinessDocument document) {

        standardBusinessDocumentRepository.save(document);

        return document;
    }
}
