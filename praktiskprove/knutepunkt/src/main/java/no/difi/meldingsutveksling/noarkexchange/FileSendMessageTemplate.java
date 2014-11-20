package no.difi.meldingsutveksling.noarkexchange;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import no.difi.meldingsutveksling.domain.SBD;

public class FileSendMessageTemplate extends SendMessageTemplate {

	@Override
	void sendSBD(SBD sbd) throws IOException {
    	FileUtils.writeByteArrayToFile(new File("sbdUt.xml"), sbd.getContent());
	}

}
