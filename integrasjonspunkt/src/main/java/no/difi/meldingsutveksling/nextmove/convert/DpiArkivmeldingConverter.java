package no.difi.meldingsutveksling.nextmove.convert;

import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.arkivverket.standarder.noark5.arkivmelding.Saksmappe;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.DpiConversationResource;

import static no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil.ARKIVMELDING_XML;

public class DpiArkivmeldingConverter implements ArkivmeldingConverter {

    @Override
    public ConversationResource convert(ConversationResource cr) {
        DpiConversationResource dpi = DpiConversationResource.of(cr);
        dpi.getFileRefs().values().removeIf(ARKIVMELDING_XML::equals);

        Arkivmelding am = cr.getArkivmelding();
        Saksmappe sm = (Saksmappe) am.getMappe().get(0);
        Journalpost jp = (Journalpost) sm.getBasisregistrering().get(0);

        dpi.setTitle(jp.getTittel());
        
        return dpi;
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPI;
    }
}
