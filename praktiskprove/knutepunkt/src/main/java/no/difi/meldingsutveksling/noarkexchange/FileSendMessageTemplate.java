package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class FileSendMessageTemplate extends SendMessageTemplateImpl {

	@Override
	void sendSBD(StandardBusinessDocument sbd) throws IOException {
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalSBD.marshal(sbd, os);
		
    	FileUtils.writeByteArrayToFile(new File(
                System.getProperty("user.home")+File.separator+"testToRemove"+File.separator+"le"+File.separator+"sbdV2.xml")
                , os.toByteArray());
	}

}
