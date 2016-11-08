package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.ptp.Document;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerClient;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerException;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createErrorResponse;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

public class PostInnbyggerMessageStrategy implements MessageStrategy {

    public static final String MPC_ID = "no.difi.move-integrasjonspunkt";
    private final ServiceRegistryLookup serviceRegistry;
    private MeldingsformidlerClient.Config config;

    public PostInnbyggerMessageStrategy(MeldingsformidlerClient.Config config, ServiceRegistryLookup serviceRegistryLookup) {
        this.config = config;
        this.serviceRegistry = serviceRegistryLookup;
    }

    @Override
    public PutMessageResponseType send(final EDUCore request) {
        request.setServiceIdentifier(ServiceIdentifier.DPI);
        final ServiceRecord serviceRecord = serviceRegistry.getServiceRecord(request.getReceiver().getIdentifier());

        MeldingsformidlerClient client = new MeldingsformidlerClient(config);
        try {
            Audit.info(String.format("Sending message to DPI with conversation id %s", request.getId()), markerFrom(request));
            client.sendMelding(new EDUCoreMeldingsformidlerRequest(request, serviceRecord));
        } catch (MeldingsformidlerException e) {
            Audit.error("Failed to send message to DPI", markerFrom(request), e);
            return createErrorResponse(StatusMessage.UNABLE_TO_SEND_DPI);
        }

        return createOkResponse();
    }

    private static class EDUCoreMeldingsformidlerRequest implements MeldingsformidlerRequest {
        public static final String NORSK_BOKMAAL = "NO";
        public static final String KAN_VARSLES = "KAN_VARSLES";
        private final EDUCore request;
        private final ServiceRecord serviceRecord;

        public EDUCoreMeldingsformidlerRequest(EDUCore request, ServiceRecord serviceRecord) {
            this.request = request;
            this.serviceRecord = serviceRecord;
        }

        @Override
        public Document getDocument() {
            final MeldingType meldingType = request.getPayloadAsMeldingType();
            final DokumentType dokumentType = meldingType.getJournpost().getDokument().get(0);
            return from(dokumentType);
        }

        private Document from(DokumentType dokumentType) {
            return new Document(dokumentType.getFil().getBase64(), dokumentType.getVeMimeType(), dokumentType.getVeFilnavn(), dokumentType.getDbTittel());
        }

        @Override
        public List<Document> getAttachments() {
            List<DokumentType> allFiles = request.getPayloadAsMeldingType().getJournpost().getDokument();
            List<Document> attachments = new ArrayList<>();
            for(int i = 1; i < allFiles.size(); i++) {
                attachments.add(from(allFiles.get(i)));
            }
            return attachments;
        }

        @Override
        public String getMottakerPid() {
            return request.getReceiver().getIdentifier();
        }

        @Override
        public String getSubject() {
            return request.getPayloadAsMeldingType().getNoarksak().getSaOfftittel(); /* TODO: er dette riktig sted og finne subject */
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
        public String getSpraakKode() {
            return NORSK_BOKMAAL;
        }

        @Override
        public String getEmail() {
            return serviceRecord.getEpostAdresse();
        }

        @Override
        public String getVarslingstekst() {
            return serviceRecord.getVarslingsStatus();
        }

        @Override
        public String getMobileNumber() {
            return serviceRecord.getMobilnummer();
        }

        @Override
        public boolean isNotifiable() {
            return serviceRecord.getVarslingsStatus().equalsIgnoreCase(KAN_VARSLES);
        }

    }
}
