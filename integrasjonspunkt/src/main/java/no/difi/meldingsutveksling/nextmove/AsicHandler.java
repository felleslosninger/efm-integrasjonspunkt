package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.NextMoveStreamedFile;
import no.difi.meldingsutveksling.domain.StreamedFile;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.services.Adresseregister;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsicHandler {

    private final IntegrasjonspunktNokkel keyHelper;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final Plumber plumber;
    private final Adresseregister adresseregister;

    InputStream createEncryptedAsic(NextMoveOutMessage msg) {

        List<NextMoveStreamedFile> attachments = msg.getFiles().stream()
                .sorted((a, b) -> {
                    if (a.getPrimaryDocument()) return -1;
                    if (b.getPrimaryDocument()) return 1;
                    return a.getDokumentnummer().compareTo(b.getDokumentnummer());
                }).map(f -> {
                    try {
                        FileEntryStream fes = optionalCryptoMessagePersister.readStream(msg.getMessageId(), f.getIdentifier());
                        return new NextMoveStreamedFile(f.getFilename(), fes.getInputStream(), getMimetype(f));
                    } catch (IOException e) {
                        throw new NextMoveRuntimeException(
                                String.format("Could not read file named '%s' for messageId = '%s'", f.getFilename(), msg.getMessageId()));
                    }
                }).collect(Collectors.toList());

        return archiveAndEncryptAttachments(attachments.get(0), attachments.stream(), msg);
    }

    private String getMimetype(BusinessMessageFile f) {
        if (StringUtils.hasText(f.getMimetype())) {
            return f.getMimetype();
        }

        String ext = Stream.of(f.getFilename().split(".")).reduce((p, e) -> e).orElse("pdf");
        return MimeTypeExtensionMapper.getMimetype(ext);
    }

    public InputStream archiveAndEncryptAttachments(StreamedFile mainAttachment, Stream<? extends StreamedFile> att, NextMoveOutMessage message) {
        CmsUtil cmsUtil = getCmsUtil(message.getServiceIdentifier());
        X509Certificate mottakerSertifikat = getMottakerSertifikat(message);

        return plumber.pipe("create asic", inlet -> createAsic(mainAttachment, att, message, inlet))
                .andThen("CMS encrypt asic", (outlet, inlet) -> cmsUtil.createCMSStreamed(outlet, inlet, mottakerSertifikat))
                .outlet();
    }

    private X509Certificate getMottakerSertifikat(NextMoveOutMessage message) {
        return (X509Certificate) adresseregister.getReceiverCertificate(message);
    }

    private void createAsic(StreamedFile mainAttachment, Stream<? extends StreamedFile> att, NextMoveOutMessage message, PipedOutputStream pipeOutlet) {
        try {
            new CreateAsice().createAsiceStreamed(mainAttachment, att, pipeOutlet, keyHelper.getSignatureHelper(), message);
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
