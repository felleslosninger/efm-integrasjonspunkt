package no.difi.meldingsutveksling.nextmove;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.nextmove.v2.Participant;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

@UtilityClass
public class HealthcareTestData {


    public final StandardBusinessDocumentTestData.MessageData DIALOGMELDING_MESSAGE_DATA = new StandardBusinessDocumentTestData.MessageData().setProcess("urn:no:difi:profile:digitalpost:fastlege:ver1.0")
        .setStandard("urn:no:difi:digitalpost:json:schema::dialogmelding").setType("dialogmelding").setBusinessMessage(new Dialogmelding().setHoveddokument("dialogmelding.json").setSikkerhetsnivaa(4));

    public static final String VALID_ORGNUMMER = "920640818";

    public static class Identifier {
        public static NhnIdentifier validNhnReceiverIdentifier = NhnIdentifier.of(VALID_ORGNUMMER,"4334","6767");
        public static NhnIdentifier validNhnSenderIdentifier = NhnIdentifier.of(VALID_ORGNUMMER,"4334","3333");
    }


    public ServiceRecord serviceRecord(NhnIdentifier nhnIdentifier) {
        return new ServiceRecord(ServiceIdentifier.DPH, nhnIdentifier.getIdentifier(), "", "")
            .setHerIdLevel1(nhnIdentifier.getHerId1())
            .setHerIdLevel2(nhnIdentifier.getHerId2());
    }

    public StandardBusinessDocument dialgmelding() {

        return createDialogMelding(Identifier.validNhnSenderIdentifier, Identifier.validNhnReceiverIdentifier);
    }



    public StandardBusinessDocument createDialogMelding(NhnIdentifier sender,NhnIdentifier receiver) {

        DIALOGMELDING_MESSAGE_DATA.setSenderIdentifier(sender.getIdentifier());
        DIALOGMELDING_MESSAGE_DATA.setReceiverIdentifier(receiver.getIdentifier());

        StandardBusinessDocument sbd = StandardBusinessDocumentTestData.getInputSbd(DIALOGMELDING_MESSAGE_DATA);
        StandardBusinessDocumentHeader header = sbd.getStandardBusinessDocumentHeader();
        header.getScopes().add(new Scope().setInstanceIdentifier(receiver.getPrimaryIdentifier().split(NhnIdentifier.IDENTIFIER_SEPARATOR)[1]).setType(ScopeType.RECEIVER_HERID2.getFullname()));
        return sbd;

    }

}
