package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.SBD;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class OxalisSendMessageTemplate extends SendMessageTemplate {

    @Override
    void sendSBD(SBD sbd) throws IOException {
    	FileUtils.writeByteArrayToFile(new File(System.getProperty("user.home") + "\\Dropbox\\DifiCmnDocs\\demo\\sbdUt.xml"), sbd.getContent());
    }
}
