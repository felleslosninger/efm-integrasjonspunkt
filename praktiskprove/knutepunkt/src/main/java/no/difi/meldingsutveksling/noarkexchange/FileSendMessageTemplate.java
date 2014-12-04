package no.difi.meldingsutveksling.noarkexchange;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

public class FileSendMessageTemplate extends SendMessageTemplate {

	@Override
	void sendSBD(StandardBusinessDocument sbd) throws IOException {
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalSBD.marshal(sbd, os);
		
    	FileUtils.writeByteArrayToFile(new File("sbdUt.xml"), os.toByteArray());
	}

}
