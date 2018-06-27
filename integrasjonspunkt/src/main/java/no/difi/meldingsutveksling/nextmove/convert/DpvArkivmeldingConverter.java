package no.difi.meldingsutveksling.nextmove.convert;

import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.arkivverket.standarder.noark5.arkivmelding.Saksmappe;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.DpvConversationResource;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil.ARKIVMELDING_XML;

@Component
public class DpvArkivmeldingConverter implements ArkivmeldingConverter {

    @Override
    public ConversationResource convert(ConversationResource cr) {
        DpvConversationResource dpv = DpvConversationResource.of(cr.getConversationId(), cr.getSenderId(), cr.getReceiverId());
        dpv.setFileRefs(cr.getFileRefs());
        dpv.getFileRefs().values().removeIf(ARKIVMELDING_XML::equals);
        dpv.setSenderName(cr.getSenderName());
        dpv.setReceiverName(cr.getReceiverName());
        dpv.setCustomProperties(cr.getCustomProperties());

        Arkivmelding am = cr.getArkivmelding();
        Saksmappe sm = (Saksmappe) am.getMappe().get(0);
        Journalpost jp = (Journalpost) sm.getBasisregistrering().get(0);

        dpv.setMessageTitle(jp.getOffentligTittel());
        dpv.setMessageContent(jp.getBeskrivelse());

        return dpv;
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return DPV;
    }
}
