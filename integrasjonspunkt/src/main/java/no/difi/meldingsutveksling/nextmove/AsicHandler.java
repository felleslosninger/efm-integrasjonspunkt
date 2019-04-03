package no.difi.meldingsutveksling.nextmove;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.NextMoveStreamedFile;
import no.difi.meldingsutveksling.domain.StreamedFile;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.noarkexchange.MessageContext;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_INNSYN;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_RECEIPT;

@Component
@Slf4j
public class AsicHandler {

    private final IntegrasjonspunktNokkel keyHelper;
    private final CryptoMessagePersister cryptoMessagePersister;

    public AsicHandler(IntegrasjonspunktNokkel keyHelper, CryptoMessagePersister cryptoMessagePersister) {
        this.keyHelper = keyHelper;
        this.cryptoMessagePersister = cryptoMessagePersister;
    }

    public InputStream createEncryptedAsic(ConversationResource cr, MessageContext messageContext) {
        List<StreamedFile> attachments = new ArrayList<>();
        if (cr.getFileRefs() != null) {
            for (String filename : cr.getFileRefs().values()) {
                FileEntryStream fileEntryStream = cryptoMessagePersister.readStream(cr.getConversationId(), filename);
                String ext = Stream.of(filename.split(".")).reduce((p, e) -> e).orElse("pdf");
                attachments.add(new NextMoveStreamedFile(filename, fileEntryStream.getInputStream(), MimeTypeExtensionMapper.getMimetype(ext)));
            }
        }

        return archiveAndEncryptAttachments(attachments.get(0), attachments.stream(), messageContext, cr.getServiceIdentifier());
    }

    public InputStream createEncryptedAsic(NextMoveMessage msg, MessageContext messageContext) {

        if (msg.getFiles() == null || msg.getFiles().isEmpty()) return null;

        List<NextMoveStreamedFile> attachments = msg.getFiles().stream()
                .sorted((a, b) -> {
                    if (a.getPrimaryDocument()) return -1;
                    if (b.getPrimaryDocument()) return 1;
                    return a.getFilename().compareTo(b.getFilename());
                }).map(f -> {
                    FileEntryStream fes = cryptoMessagePersister.readStream(msg.getConversationId(), f.getIdentifier());
                    return new NextMoveStreamedFile(f.getFilename(), fes.getInputStream(), getMimetype(f));
                }).collect(Collectors.toList());

        return archiveAndEncryptAttachments(attachments.get(0), attachments.stream(), messageContext, msg.getServiceIdentifier());
    }

    private String getMimetype(BusinessMessageFile f) {
        String mimetype;
        if (Strings.isNullOrEmpty(f.getMimetype())) {
            String ext = Stream.of(f.getFilename().split(".")).reduce((p, e) -> e).orElse("pdf");
            mimetype = MimeTypeExtensionMapper.getMimetype(ext);
        } else {
            mimetype = f.getMimetype();
        }
        return mimetype;
    }

    public InputStream archiveAndEncryptAttachments(StreamedFile mainAttachment, Stream<? extends StreamedFile> att, MessageContext ctx, ServiceIdentifier si) {
        PipedOutputStream archiveOutputStream = new PipedOutputStream();
        PipedInputStream archiveInputStream;
        try {
            archiveInputStream = new PipedInputStream(archiveOutputStream);
        } catch (IOException e) {
            String errorMsg = "Error creating PipedInputStream from ASiC";
            log.error(errorMsg);
            throw new MeldingsUtvekslingRuntimeException(errorMsg, e);
        }

        CompletableFuture.runAsync(() -> {
            log.trace("Starting thread: create asic");
            try {
                new CreateAsice().createAsiceStreamed(mainAttachment, att, archiveOutputStream, keyHelper.getSignatureHelper(),
                        ctx.getAvsender(), ctx.getMottaker());

                archiveOutputStream.flush();
                archiveOutputStream.close();
            } catch (IOException e) {
                throw new MeldingsUtvekslingRuntimeException(StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT.getTechnicalMessage(), e);
            }
            log.trace("Thread finished: create asic");
        });

        PipedOutputStream encryptedOutputStream = new PipedOutputStream();
        PipedInputStream encryptedInputStream;
        try {
            encryptedInputStream = new PipedInputStream(encryptedOutputStream);
        } catch (IOException e) {
            String errorMsg = "Error creating PipedInputStream from encrypted ASiC";
            log.error(errorMsg);
            throw new MeldingsUtvekslingRuntimeException(errorMsg, e);
        }

        CompletableFuture.runAsync(() -> {
            log.debug("Starting thread: encrypt archive");
            encryptArchive(ctx.getMottaker(), si, archiveInputStream, encryptedOutputStream);
            try {
                encryptedOutputStream.flush();
                encryptedOutputStream.close();
            } catch (IOException e) {
                log.error("Error closing encryption stream");
            }
            log.debug("Thread finished: encrypt archive");
        });

        return encryptedInputStream;
    }

    private void encryptArchive(Mottaker mottaker, ServiceIdentifier serviceIdentifier, InputStream archive, OutputStream encrypted) {
        Set<ServiceIdentifier> standardEncryptionUsers = EnumSet.of(DPE_INNSYN, DPE_RECEIPT);

        CmsUtil cmsUtil;
        if (standardEncryptionUsers.contains(serviceIdentifier)) {

            cmsUtil = new CmsUtil(null);
        } else {

            cmsUtil = new CmsUtil();
        }

        cmsUtil.createCMSStreamed(archive, encrypted, (X509Certificate) mottaker.getSertifikat());
    }
}
