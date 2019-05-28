package no.difi.meldingsutveksling.noarkexchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.domain.*;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.AsicHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;
import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.payloadSizeMarker;

/**
 * Factory class for StandardBusinessDocument instances
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StandardBusinessDocumentFactory {

    private final IntegrasjonspunktNokkel integrasjonspunktNokkel;
    private final AsicHandler asicHandler;
    private final CreateSBD createSBD;

    public StandardBusinessDocument create(EDUCore sender, Avsender avsender, Mottaker mottaker) throws MessageException {
        return create(sender, UUID.randomUUID().toString(), avsender, mottaker);
    }

    public StandardBusinessDocument create(EDUCore shipment, String conversationId, Avsender avsender, Mottaker mottaker) throws MessageException {
        byte[] marshalledShipment = EDUCoreConverter.marshallToBytes(shipment);

        BestEduMessage bestEduMessage = new BestEduMessage(marshalledShipment);
        LogstashMarker marker = markerFrom(shipment);
        Audit.info("Payload size", marker.and(payloadSizeMarker(marshalledShipment)));
        Archive archive;
        try {
            archive = createAsicePackage(avsender, mottaker, bestEduMessage);
        } catch (IOException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT);
        }
        Payload payload = new Payload(encryptArchive(mottaker, archive, shipment.getServiceIdentifier()));

        return createSBD.createSBD(
                avsender.getOrgNummer(),
                mottaker.getOrgNummer(),
                payload,
                conversationId,
                DocumentType.BESTEDU_MELDING,
                shipment.getJournalpostId());
    }

    private byte[] encryptArchive(Mottaker mottaker, Archive archive, ServiceIdentifier serviceIdentifier) {
        CmsUtil cmsUtil;
        if (DPE == serviceIdentifier) {
            cmsUtil = new CmsUtil(null);
        } else {
            cmsUtil = new CmsUtil();
        }

        return cmsUtil.createCMS(archive.getBytes(), (X509Certificate) mottaker.getSertifikat());
    }

    private Archive createAsicePackage(Avsender avsender, Mottaker mottaker, ByteArrayFile byteArrayFile) throws
            IOException {
        return new CreateAsice().createAsice(byteArrayFile, integrasjonspunktNokkel.getSignatureHelper(), avsender, mottaker);
    }
}