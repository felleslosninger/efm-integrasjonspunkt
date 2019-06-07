package no.difi.meldingsutveksling.nextmove;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.NextMoveStreamedFile;
import no.difi.meldingsutveksling.domain.StreamedFile;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.noarkexchange.MessageContext;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.pipes.Pipe;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;

@Component
@Slf4j
public class AsicHandler {

    private final IntegrasjonspunktNokkel keyHelper;
    private final CryptoMessagePersister cryptoMessagePersister;

    public AsicHandler(IntegrasjonspunktNokkel keyHelper, CryptoMessagePersister cryptoMessagePersister) {
        this.keyHelper = keyHelper;
        this.cryptoMessagePersister = cryptoMessagePersister;
    }

    public InputStream createEncryptedAsic(NextMoveOutMessage msg, MessageContext messageContext) {

        if (msg.getFiles() == null || msg.getFiles().isEmpty()) return null;

        List<NextMoveStreamedFile> attachments = msg.getFiles().stream()
                .sorted((a, b) -> {
                    if (a.getPrimaryDocument()) return -1;
                    if (b.getPrimaryDocument()) return 1;
                    return a.getFilename().compareTo(b.getFilename());
                }).map(f -> {
                    try {
                        FileEntryStream fes = cryptoMessagePersister.readStream(msg.getConversationId(), f.getIdentifier());
                        return new NextMoveStreamedFile(f.getFilename(), fes.getInputStream(), getMimetype(f));
                    } catch (IOException e) {
                        throw new NextMoveRuntimeException(
                                String.format("Could not read file named '%s' for conversationId = '%s'", f.getFilename(), msg.getConversationId()));
                    }
                }).collect(Collectors.toList());

        return archiveAndEncryptAttachments(attachments.get(0), attachments.stream(), messageContext, msg.getServiceIdentifier());
    }

    private String getMimetype(BusinessMessageFile f) {
        if (Strings.isNullOrEmpty(f.getMimetype())) {
            String ext = Stream.of(f.getFilename().split(".")).reduce((p, e) -> e).orElse("pdf");
            return MimeTypeExtensionMapper.getMimetype(ext);
        }

        return f.getMimetype();
    }

    public InputStream archiveAndEncryptAttachments(StreamedFile mainAttachment, Stream<? extends StreamedFile> att, MessageContext ctx, ServiceIdentifier si) {
        CmsUtil cmsUtil = getCmsUtil(si);
        X509Certificate mottakerSertifikat = getMottakerSertifikat(ctx);

        return Pipe.of("create asic", inlet -> createAsic(mainAttachment, att, ctx, inlet))
                .andThen("CMS encrypt asic", (outlet, inlet) -> cmsUtil.createCMSStreamed(outlet, inlet, mottakerSertifikat))
                .outlet();
    }

    private X509Certificate getMottakerSertifikat(MessageContext ctx) {
        return (X509Certificate) ctx.getMottaker().getSertifikat();
    }

    private void createAsic(StreamedFile mainAttachment, Stream<? extends StreamedFile> att, MessageContext ctx, PipedOutputStream pipeOutlet) {
        try {
            new CreateAsice().createAsiceStreamed(mainAttachment, att, pipeOutlet, keyHelper.getSignatureHelper(),
                    ctx.getAvsender(), ctx.getMottaker());
        } catch (IOException e) {
            throw new MeldingsUtvekslingRuntimeException(StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT.getTechnicalMessage(), e);
        }
    }

    private CmsUtil getCmsUtil(ServiceIdentifier serviceIdentifier) {
        if (DPE == serviceIdentifier) {
            return new CmsUtil(null);
        }

        return new CmsUtil();
    }
}
