package no.difi.meldingsutveksling.nextbest;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
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

    public void enqueueEduDocument(EduDocument eduDocument) throws IOException, MessageException {

        if (!(eduDocument.getAny() instanceof Payload)) {
            log.error("Message attachement not instance of Payload.");
            throw new MeldingsUtvekslingRuntimeException("Message attachement ("+eduDocument.getAny()+") not instance of " +
                    ""+Payload.class);
        }

        byte[] decryptedAsicPackage = decrypt((Payload)eduDocument.getAny());
        Map<String, byte[]> contentFromAsic = getContentFromAsic(decryptedAsicPackage);

        IncomingConversationResource message = IncomingConversationResource.of(eduDocument.getConversationId(),
                eduDocument.getSenderOrgNumber(),
                eduDocument.getReceiverOrgNumber(), "OEP");

        contentFromAsic.keySet().forEach(k -> message.addFileRef(k));

        String filedir = props.getNextbest().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir+"/";
        }
        filedir = filedir+eduDocument.getConversationId()+"/";
        File localFile = new File(filedir+"message.zip");
        localFile.getParentFile().mkdirs();

        try {
            FileOutputStream os = new FileOutputStream(localFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            bos.write(decryptedAsicPackage);
            bos.close();

            repoIn.save(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public byte[] decrypt(Payload payload) {
        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        CmsUtil cmsUtil = new CmsUtil();
        return cmsUtil.decryptCMS(cmsEncZip, keyInfo.loadPrivateKey());
    }

    public Map<String, byte[]> getContentFromAsic(byte[] bytes) throws IOException, MessageException {
        Map<String, byte[]> files = new HashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                files.put(entry.getName(), IOUtils.toByteArray(zipInputStream));
            }
        } catch (Exception e) {
            throw new MessageException(StatusMessage.UNABLE_TO_EXTRACT_ZIP_CONTENTS);
        }
        return files;
    }
}
