package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.SBD;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Profile("dev")
public class OxalisSendMessageTemplate extends SendMessageTemplate {

    @Override
    void sendSBD(SBD sbd) throws IOException {
        FileUtils.writeByteArrayToFile(new File(System.getProperty("user.home") + "/" + "sbdUt.xml"), sbd.getContent());
    }
}
