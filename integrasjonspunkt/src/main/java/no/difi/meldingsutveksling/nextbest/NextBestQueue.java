package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.Decryptor;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
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

@Component
public class NextBestQueue {

    private static final Logger log = LoggerFactory.getLogger(NextBestQueue.class);

    @Autowired
    private IncomingConversationResourceRepository repoIn;

    @Autowired
    private OutgoingConversationResourceRepository repoOut;

    @Autowired
    private IntegrasjonspunktNokkel keyInfo;

    @Autowired
    private IntegrasjonspunktProperties props;

    public void enqueueEduDocument(EduDocument eduDocument) {

        if (!(eduDocument.getAny() instanceof Payload)) {
            log.error("Message attachement not instance of Payload.");
            throw new MeldingsUtvekslingRuntimeException("Message attachement ("+eduDocument.getAny()+") not instance of " +
                    ""+Payload.class);
        }

        byte[] decryptedAsicPackage = decrypt((Payload)eduDocument.getAny());
        List<String> contentFromAsic = null;
        try {
            contentFromAsic = getContentFromAsic(decryptedAsicPackage);
        } catch (MessageException e) {
            log.error("Could not get contents from asic", e);
            throw new MeldingsUtvekslingRuntimeException("Could not get contents from asic", e);
        }

        IncomingConversationResource message = IncomingConversationResource.of(eduDocument.getConversationId(),
                eduDocument.getSenderOrgNumber(),
                eduDocument.getReceiverOrgNumber(), eduDocument.getMessagetypeId());

        message.addFileRef(props.getNextbest().getAsicfile());
        contentFromAsic.forEach(message::addFileRef);

        String filedir = props.getNextbest().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir+"/";
        }
        filedir = filedir+eduDocument.getConversationId()+"/";
        File localFile = new File(filedir+props.getNextbest().getAsicfile());
        localFile.getParentFile().mkdirs();

        try {
            FileOutputStream os = new FileOutputStream(localFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            bos.write(decryptedAsicPackage);
            bos.close();
            os.close();

            repoIn.save(message);
        } catch (IOException e) {
            log.error("Could not write asic container to disc.", e);
        }

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
