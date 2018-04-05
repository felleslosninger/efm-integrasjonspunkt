package no.difi.meldingsutveksling.nextmove.validation;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.DpiConversationResource;
import no.difi.meldingsutveksling.nextmove.FileAttachement;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DpiConversationValidator implements ConversationValidator {
    @Override
    public void validate(ConversationResource cr) throws NextMoveException {
        if (!(cr instanceof DpiConversationResource)) {
            throw new DpiValidationException(String.format("DPI validator called with {}, not compatible", cr.getClass().getName()));
        }
        DpiConversationResource dpiCr = (DpiConversationResource) cr;

        // One file attachment must always be set to 'hoveddokument'
        if (dpiCr.getFiles().stream().noneMatch(FileAttachement::isHoveddokument)) {
            throw new DpiValidationException("No 'hoveddokument' supplied in file attachments, cannot continue");
        }

        // Must have field 'ikkeSensitivTittel'
        if (dpiCr.getDigitalPostInfo() != null && Strings.isNullOrEmpty(dpiCr.getDigitalPostInfo().getIkkeSensitivTittel())) {
            throw new DpiValidationException("Field 'ikkeSensitivTittel' has no value, cannot continue");
        }

    }

    @Override
    public ServiceIdentifier getServicIdentifier() {
        return ServiceIdentifier.DPI;
    }
}
