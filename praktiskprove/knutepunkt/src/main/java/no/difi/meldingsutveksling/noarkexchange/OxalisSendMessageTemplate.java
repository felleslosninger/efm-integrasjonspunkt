package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.DemoPakking;
import no.difi.meldingsutveksling.domain.SBD;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class OxalisSendMessageTemplate extends SendMessageTemplate {


    @Override
    SBD createSBD(PutMessageRequestType sender) {
    	return new DemoPakking().wrapContentInSPD(sender.getPayload());
    }

    @Override
    void sendSBD(SBD sbd) throws IOException {
    	FileUtils.writeByteArrayToFile(new File("sbdUt.xml"), sbd.content);
    }
}
