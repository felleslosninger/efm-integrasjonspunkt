package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

public class HealthcareTestData {


    public final static StandardBusinessDocumentTestData.MessageData DIALOGMELDING_MESSAGE_DATA = new StandardBusinessDocumentTestData.MessageData().setProcess("urn:no:difi:profile:digitalpost:fastlege:ver1.0")
        .setStandard("urn:no:difi:digitalpost:json:schema::dialogmelding").setType("dialogmelding").setBusinessMessage(new Dialogmelding().setHoveddokument("dialogmelding.json").setSikkerhetsnivaa(4));

    public static final String VALID_ORGNUMMER = "920640818";
    public static final String VALID_FNR = "21905297101";

    public static class Identifier {
        public static NhnIdentifier validFastlegeReceiverIdentifier = NhnIdentifier.of(VALID_FNR,"6791","9898");
        public static NhnIdentifier validNhnReceiverIdentifier = NhnIdentifier.of(VALID_ORGNUMMER,"4334","6767");
        public static NhnIdentifier validNhnSenderIdentifier = NhnIdentifier.of(VALID_ORGNUMMER,"4334","3333");
    }


    public static ServiceRecord serviceRecord(NhnIdentifier nhnIdentifier) {
        return new ServiceRecord(ServiceIdentifier.DPH, nhnIdentifier.getIdentifier(), "", "")
            .setHerIdLevel1(nhnIdentifier.getHerId1())
            .setHerIdLevel2(nhnIdentifier.getHerId2());
    }

    public static StandardBusinessDocument dialgmelding() {

        return createDialogMelding(Identifier.validNhnSenderIdentifier, Identifier.validNhnReceiverIdentifier);
    }



    public static StandardBusinessDocument createDialogMelding(NhnIdentifier sender,NhnIdentifier receiver) {

        DIALOGMELDING_MESSAGE_DATA.setSenderIdentifier(sender.getIdentifier());
        DIALOGMELDING_MESSAGE_DATA.setReceiverIdentifier(receiver.getIdentifier());

        StandardBusinessDocument sbd = StandardBusinessDocumentTestData.getInputSbd(DIALOGMELDING_MESSAGE_DATA);
        StandardBusinessDocumentHeader header = sbd.getStandardBusinessDocumentHeader();
        header.getScopes().add(new Scope().setInstanceIdentifier(sender.getPrimaryIdentifier().split(NhnIdentifier.IDENTIFIER_SEPARATOR)[1]).setType(ScopeType.SENDER_HERID2.getFullname()));
        if (receiver.isNhnPartnerIdentifier()) {
            header.getScopes().add(new Scope().setInstanceIdentifier(receiver.getPrimaryIdentifier().split(NhnIdentifier.IDENTIFIER_SEPARATOR)[1]).setType(ScopeType.RECEIVER_HERID2.getFullname()));
        }
        return sbd;

    }

}
