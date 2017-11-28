package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.Document;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createErrorResponse;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

public class PostInnbyggerMessageStrategy implements MessageStrategy {

    public static final String MPC_ID = "no.difi.move-integrasjonspunkt";
    private final ServiceRegistryLookup serviceRegistry;
    private KeyStore keyStore;
    private DigitalPostInnbyggerConfig config;

    PostInnbyggerMessageStrategy(DigitalPostInnbyggerConfig config, ServiceRegistryLookup serviceRegistryLookup, KeyStore keyStore) {
        this.config = config;
        this.serviceRegistry = serviceRegistryLookup;
        this.keyStore = keyStore;
    }

    @Override
    public PutMessageResponseType send(final EDUCore request) {
        Optional<ServiceRecord> serviceRecord = serviceRegistry.getServiceRecord(request.getReceiver().getIdentifier(), ServiceIdentifier.DPI);
        if (!serviceRecord.isPresent()) {
            throw new MeldingsUtvekslingRuntimeException(String.format("Receiver %s does not have ServiceRecord of type DPI", request.getReceiver().getIdentifier()));
        }

        MeldingsformidlerClient client = new MeldingsformidlerClient(config, keyStore);
        try {
            Audit.info(String.format("Sending message to DPI with conversation id %s", request.getId()), markerFrom(request));
            client.sendMelding(new EDUCoreMeldingsformidlerRequest(config, request, serviceRecord.get()));
        } catch (MeldingsformidlerException e) {
            Audit.error("Failed to send message to DPI", markerFrom(request), e);
            return createErrorResponse(StatusMessage.UNABLE_TO_SEND_DPI);
        }

        return createOkResponse();
    }

    @Override
    public String serviceName() {
        return "DPI";
    }

    private static class EDUCoreMeldingsformidlerRequest implements MeldingsformidlerRequest {
        static final String KAN_VARSLES = "KAN_VARSLES";
        private final DigitalPostInnbyggerConfig config;
        private final EDUCore request;
        private final MeldingType meldingType;
        private final ServiceRecord serviceRecord;

        EDUCoreMeldingsformidlerRequest(DigitalPostInnbyggerConfig config, EDUCore request, ServiceRecord serviceRecord) {
            this.config = config;
            this.request = request;
            this.serviceRecord = serviceRecord;
            this.meldingType = EDUCoreConverter.payloadAsMeldingType(request.getPayload());
        }

        @Override
        public Document getDocument() {
            final DokumentType dokumentType = meldingType.getJournpost().getDokument().get(0);
            final String tittel = getSubject();
            return createDocument(tittel, dokumentType);
        }

        private Document createDocument(String tittel, DokumentType dokumentType) {
            return new Document(dokumentType.getFil().getBase64(), dokumentType.getVeMimeType(), dokumentType.getVeFilnavn(), tittel);
        }

        @Override
        public List<Document> getAttachments() {
            List<DokumentType> allFiles = meldingType.getJournpost().getDokument();
            List<Document> attachments = new ArrayList<>();
            for(int i = 1; i < allFiles.size(); i++) {
                final DokumentType a = allFiles.get(i);
                attachments.add(createDocument(a.getDbTittel(), a));
            }
            return attachments;
        }

        @Override
        public String getMottakerPid() {
            return request.getReceiver().getIdentifier();
        }

        @Override
        public String getSubject() {
            return meldingType.getJournpost().getJpOffinnhold();
        }

        @Override
        public String getSenderOrgnumber() {
            return request.getSender().getIdentifier();
        }

        @Override
        public String getConversationId() {
            return request.getId();
        }

        @Override
        public String getPostkasseAdresse() {
            return serviceRecord.getPostkasseAdresse(); /* fra KRR via SR */
        }

        @Override
        public byte[] getCertificate() {
            try {
                return serviceRecord.getPemCertificate().getBytes("UTF-8"); /* fra KRR via SR */
            } catch (UnsupportedEncodingException e) {
                throw new MeldingsUtvekslingRuntimeException("Pem certificate from servicerecord problems", e);
            }
        }

        @Override
        public String getOrgnrPostkasse() {
            return serviceRecord.getOrgnrPostkasse(); /* fra KRR via SR */
        }

        @Override
        public String getEmailAddress() {
            return serviceRecord.getEpostAdresse();
        }

        @Override
        public String getSmsVarslingstekst() {
            return config.getSms().getVarslingstekst();
        }

        @Override
        public String getEmailVarslingstekst() {
            return config.getEmail().getVarslingstekst();
        }

        @Override
        public String getMobileNumber() {
            return serviceRecord.getMobilnummer();
        }

        @Override
        public boolean isNotifiable() {
            return serviceRecord.isKanVarsles();
        }

        @Override
        public boolean isPrintProvider() {
            return serviceRecord.isFysiskPost();
        }

        @Override
        public PostAddress getPostAddress() {
            return serviceRecord.getPostAddress();
        }

        @Override
        public PostAddress getReturnAddress() {
            return serviceRecord.getReturnAddress();
        }


    }
}
