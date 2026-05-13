package no.difi.meldingsutveksling.dph.client.internal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dph.client.domain.SendApplicationReceiptInput;
import no.difi.meldingsutveksling.dph.client.domain.SendBusinessDocumentInput;
import no.difi.meldingsutveksling.nextmove.DialogmeldingKvitteringMessage;
import no.difi.meldingsutveksling.nextmove.DialogmeldingKvitteringStatus;
import no.difi.meldingsutveksling.nextmove.DialogmeldingMessage;
import no.difi.meldingsutveksling.nextmove.KvitteringStatusMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.AttachmentMetadata;
import no.difi.meldingsutveksling.nhn.adapter.model.OutgoingApplicationReceipt;
import no.difi.meldingsutveksling.nhn.adapter.model.OutgoingBusinessDocument;
import no.difi.meldingsutveksling.nhn.adapter.model.Pasient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DphDocumentConverter {

    public no.difi.meldingsutveksling.nhn.adapter.model.OutgoingBusinessDocument toExternal(SendBusinessDocumentInput input) {
        DialogmeldingMessage in = input.getPayload();
        DialogmeldingMessage.Pasient pasient = in.getPasient();

        return new OutgoingBusinessDocument(
            input.getMessageId(),
            input.getConversationId(),
            input.getParentId(),
            input.getSenderHerId(),
            input.getReceiverHerId(),
            new no.difi.meldingsutveksling.nhn.adapter.model.DialogmeldingMessage(in.getHoveddokument(),
                new no.difi.meldingsutveksling.nhn.adapter.model.Pasient(pasient.getFnr(), pasient.getFornavn(), pasient.getMellomnavn(), pasient.getEtternavn()),
                toExternal(in.getMetadataFiler())));
    }

    private Map<String, AttachmentMetadata> toExternal(@NotNull @NotEmpty Map<String, DialogmeldingMessage.Metadata> in) {
        return in.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            p -> new AttachmentMetadata(
                Optional.ofNullable(p.getValue().getIssueDate()).map(Instant::toString).orElse(null),
                p.getValue().getDescription())));
    }

    public OutgoingApplicationReceipt toExternal(SendApplicationReceiptInput input) {
        return new OutgoingApplicationReceipt(
            input.getSenderHerId(),
            toExternal(input.getPayload())
        );
    }

    private no.difi.meldingsutveksling.nhn.adapter.model.DialogmeldingKvitteringMessage toExternal(DialogmeldingKvitteringMessage in) {
        return new no.difi.meldingsutveksling.nhn.adapter.model.DialogmeldingKvitteringMessage(
            in.getRelatedToMessageId(),
            no.difi.meldingsutveksling.nhn.adapter.model.DialogmeldingKvitteringStatus.valueOf(in.getStatus().name()),
            toExternal(in.getMessages()),
            null
        );
    }

    private List<no.difi.meldingsutveksling.nhn.adapter.model.KvitteringStatusMessage> toExternal(Set<KvitteringStatusMessage> messages) {
        if (messages == null) {
            return null;
        }

        return messages.stream().map(this::toExternal).toList();
    }

    private no.difi.meldingsutveksling.nhn.adapter.model.KvitteringStatusMessage toExternal(KvitteringStatusMessage in) {
        return new no.difi.meldingsutveksling.nhn.adapter.model.KvitteringStatusMessage(in.getCode(), in.getText());
    }

    public DialogmeldingMessage toInternal(no.difi.meldingsutveksling.nhn.adapter.model.DialogmeldingMessage in) {
        return new DialogmeldingMessage()
            .setHoveddokument(in.getHoveddokument())
            .setMetadataFiler(toInternal(in.getMetadataFiler()))
            .setPasient(toInternal(in.getPasient()));
    }

    private Map<String, DialogmeldingMessage.Metadata> toInternal(Map<String, AttachmentMetadata> in) {
        if (in == null) {
            return null;
        }

        return in.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            p -> new DialogmeldingMessage.Metadata()
                .setIssueDate(Optional.ofNullable(p.getValue().getIssueDate()).map(Instant::parse).orElse(null))
                .setDescription(p.getValue().getDescription())));
    }

    private DialogmeldingMessage.Pasient toInternal(Pasient in) {
        if (in == null) {
            return null;
        }

        return new DialogmeldingMessage.Pasient()
            .setFnr(in.getFnr())
            .setFornavn(in.getFornavn())
            .setMellomnavn(in.getMellomnavn())
            .setEtternavn(in.getEtternavn());
    }

    public DialogmeldingKvitteringMessage toInternal(no.difi.meldingsutveksling.nhn.adapter.model.DialogmeldingKvitteringMessage in) {
        return new DialogmeldingKvitteringMessage()
            .setRelatedToMessageId(in.getRelatedToMessageId())
            .setStatus(DialogmeldingKvitteringStatus.valueOf(in.getStatus().name()))
            .setMessages(toInternal(in.getMessages()))
            .setHoveddokument(in.getHoveddokument());
    }

    private Set<KvitteringStatusMessage> toInternal(List<no.difi.meldingsutveksling.nhn.adapter.model.KvitteringStatusMessage> messages) {
        if (messages == null) {
            return null;
        }

        return messages.stream()
            .map(this::toInternal)
            .collect(Collectors.toUnmodifiableSet());
    }

    private KvitteringStatusMessage toInternal(no.difi.meldingsutveksling.nhn.adapter.model.KvitteringStatusMessage p) {
        return new KvitteringStatusMessage()
            .setCode(p.getCode())
            .setText(p.getText());
    }
}
