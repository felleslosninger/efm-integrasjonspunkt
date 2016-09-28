package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.ptp.Document;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerClient;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerException;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;

import java.util.List;
import java.util.UUID;

public class PostInnbyggerMessageStrategy implements MessageStrategy {

    private final ServiceRegistryLookup serviceRegistry;
    private MeldingsformidlerClient.Config config;

    public PostInnbyggerMessageStrategy(MeldingsformidlerClient.Config config, ServiceRegistryLookup serviceRegistryLookup) {
        this.config = config;
        this.serviceRegistry = serviceRegistryLookup;
    }

    @Override
    public PutMessageResponseType putMessage(final EDUCore request) {
        final MeldingsformidlerClient.Config config = this.config;
        MeldingsformidlerClient client = new MeldingsformidlerClient(config);
        try {
            client.sendMelding(new MeldingsformidlerRequest() {
                @Override
                public Document getDocument() {
                    final MeldingType meldingType = request.getPayloadAsMeldingType();
                    final DokumentType dokumentType = meldingType.getJournpost().getDokument().get(0);
                    return new Document(dokumentType.getFil().getBase64(), dokumentType.getVeMimeType(), dokumentType.getVeFilnavn(), dokumentType.getDbTittel());
                }

                @Override
                public List<Document> getAttachements() {
                    return null;
                }

                @Override
                public String getMottakerPid() {
                    return request.getReceiver().getOrgNr();
                }

                @Override
                public String getSubject() {
                    return request.getPayloadAsMeldingType().getNoarksak().getSaOfftittel(); /* TODO: er dette riktig sted og finne subject */
                }

                @Override
                public String getSenderOrgnumber() {
                    return request.getSender().getOrgNr();
                }

                @Override
                public String getConversationId() {
                    return String.valueOf(UUID.randomUUID()); /* TODO: finnes denne i EduCore? */
                }

                @Override
                public String getPostkasseAdresse() {
                    return null; /* fra KRR via SR */
                }

                @Override
                public byte[] getCertificate() {
                    return new byte[0]; /* fra KRR via SR */
                }

                @Override
                public String getOrgnrPostkasse() {
                    return null; /* fra KRR via SR */
                }

                @Override
                public String getSpraakKode() {
                    return null; /* TODO: hvor hentes denne fra? EduCore? */
                }

                @Override
                public String getQueueId() {
                    return "queueId"; /* TODO: hva skal denne egentlig settes til? */
                }

            });
        } catch (MeldingsformidlerException e) {
            //TODO
        }
        return null;
    }
}
