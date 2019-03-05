package no.difi.meldingsutveksling.nextmove.v2;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@Validated
@Api
@RequestMapping("/api/message/out")
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageOutController {

    private final StandardBusinessDocumentRepository sbdRepo;
    private final NextMoveMessageRepository messageRepo;
    private final NextMoveMessageService messageService;

    @PostMapping
    @ApiOperation(value = "Create message", notes = "Create a new messagee with the given values")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument createMessage(
            @Valid @RequestBody StandardBusinessDocument sbd) throws NextMoveException {

        sbd = messageService.setDefaults(sbd);
        NextMoveMessage message = NextMoveMessage.of(sbd.getConversationId(), sbd.getReceiverOrgNumber(), sbd);
        messageRepo.save(message);

        return sbd;
    }


    @GetMapping
    @ApiOperation(value = "Get all messages", notes = "Returns all queued messages")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public List<StandardBusinessDocument> getAllMessages() {
        return sbdRepo.findAll();
    }


    @GetMapping("/{conversationId}")
    @ApiOperation(value = "Get all messages", notes = "Returns all queued messages")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public ResponseEntity getMessage(
            @PathVariable("conversationId") String conversationId) {
        Optional<NextMoveMessage> message = messageRepo.findByConversationId(conversationId);
        if (message.isPresent()) {
            return ResponseEntity.ok(message.get().getSbd());
        }
        return ResponseEntity.notFound().build();
    }

}
