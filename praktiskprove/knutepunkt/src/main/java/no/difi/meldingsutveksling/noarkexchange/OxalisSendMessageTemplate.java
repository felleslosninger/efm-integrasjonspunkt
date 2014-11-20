package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.DemoPakking;
import no.difi.meldingsutveksling.domain.SBD;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
class OxalisSendMessageTemplate extends SendMessageTemplate {

    @Override
    SBD createSBD(PutMessageRequestType sender) {
        return new DemoPakking().wrapContentInSPD(sender.getPayload());
    }

    @Override
    void sendSBD(SBD sbd) throws IOException {
        FileUtils.writeByteArrayToFile(new File(System.getProperty("user.home") + "\\Dropbox\\DifiCmnDocs\\demo\\sbdUt.xml"), sbd.content);
    }
}
