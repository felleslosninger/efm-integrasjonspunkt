package no.difi.meldingsutveksling.nextmove;

import lombok.extern.log4j.Log4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Log4j
public class NextMoveUtils {

    private IntegrasjonspunktProperties props;

    @Autowired
    public NextMoveUtils(IntegrasjonspunktProperties props) {
        this.props = props;
    }

    public String getConversationFiledirPath(ConversationResource cr) {
        String filedir = props.getNextmove().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir+"/";
        }
        return filedir+cr.getConversationId()+"/";
    }

    public void deleteFiles(ConversationResource cr) {
        File dir = new File(getConversationFiledirPath(cr));
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            log.error("Could not delete directory: "+dir.getAbsolutePath(), e);
        }
    }

}
