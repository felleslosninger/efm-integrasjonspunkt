package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Component
@Profile("dev")
public class FileSendMessageTemplate extends SendMessageTemplate {

	@Override
	void sendSBD(StandardBusinessDocument sbd) throws IOException {
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalSBD.marshal(sbd, os);
		
    	FileUtils.writeByteArrayToFile(new File(
                System.getProperty("user.home")+File.separator+"testToRemove"+File.separator+"le"+File.separator+"sbdV2.xml")
                , os.toByteArray());
	}

}
