package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import no.difi.meldingsutveksling.Decryptor;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;

@Component
public class NextMoveQueue {

    private static final Logger log = LoggerFactory.getLogger(NextMoveQueue.class);

    private DirectionalConversationResourceRepository inRepo;

    @Autowired
    private IntegrasjonspunktNokkel keyInfo;

    @Autowired
    private IntegrasjonspunktProperties props;

    @Autowired
    public NextMoveQueue(ConversationResourceRepository repo) {
        inRepo = new DirectionalConversationResourceRepository(repo, INCOMING);
    }

    public void enqueueEduDocument(EduDocument eduDocument) {

        if (!(eduDocument.getAny() instanceof Payload)) {
            log.error("Message attachement not instance of Payload.");
            throw new MeldingsUtvekslingRuntimeException("Message attachement ("+eduDocument.getAny()+") not instance of " +
                    ""+Payload.class);
        }

        byte[] decryptedAsicPackage = decrypt((Payload)eduDocument.getAny());
        List<String> contentFromAsic;
        try {
            contentFromAsic = getContentFromAsic(decryptedAsicPackage);
        } catch (MessageException e) {
            log.error("Could not get contents from asic", e);
            throw new MeldingsUtvekslingRuntimeException("Could not get contents from asic", e);
        }

        ConversationResource message = ((Payload) eduDocument.getAny()).getConversation();

        message.setFileRefs(Maps.newHashMap());
        message.addFileRef(props.getNextbest().getAsicfile());
        contentFromAsic.forEach(message::addFileRef);

        String filedir = props.getNextbest().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir+"/";
        }
        filedir = filedir+eduDocument.getConversationId()+"/";
        File localFile = new File(filedir+props.getNextbest().getAsicfile());
        localFile.getParentFile().mkdirs();

        try (FileOutputStream os = new FileOutputStream(localFile);
            BufferedOutputStream bos = new BufferedOutputStream(os)) {
            bos.write(decryptedAsicPackage);
            inRepo.save(message);
        } catch (IOException e) {
            log.error("Could not write asic container to disc.", e);
        }
        Audit.info(String.format("Message with id=%s put on local queue", message.getConversationId()));
    }

    public byte[] decrypt(Payload payload) {
        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        return new Decryptor(keyInfo).decrypt(cmsEncZip);
    }

    public List<String> getContentFromAsic(byte[] bytes) throws MessageException {
        List<String> files = Lists.newArrayList();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                files.add(entry.getName());
            }
        } catch (Exception e) {
            log.error("Failed reading entries in asic.", e);
            throw new MessageException(StatusMessage.UNABLE_TO_EXTRACT_ZIP_CONTENTS);
        }
        return files;
    }

}
