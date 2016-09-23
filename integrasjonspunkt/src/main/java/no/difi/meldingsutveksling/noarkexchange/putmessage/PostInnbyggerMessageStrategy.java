package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.ptp.Document;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerClient;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerException;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerRequest;

import java.util.List;

public class PostInnbyggerMessageStrategy implements MessageStrategy {

    private MeldingsformidlerClient.Config config;

    public PostInnbyggerMessageStrategy(MeldingsformidlerClient.Config config) {
        this.config = config;
    }

    @Override
    public PutMessageResponseType putMessage(EDUCore request) {
        final MeldingsformidlerClient.Config config = this.config;

        MeldingsformidlerClient client = new MeldingsformidlerClient(config);
        try {
            client.sendMelding(new MeldingsformidlerRequest() {
                @Override
                public Document getDocument() {
    //                final MeldingType payloadAsMeldingType = request.getPayloadAsMeldingType().getJournpost().getDokument();

                    return null;

                }

                @Override
                public List<Document> getAttachements() {
                    return null;
                }

                @Override
                public String getMottakerPid() {
                    return null;
                }

                @Override
                public String getSubject() {
                    return null;
                }

                @Override
                public String getDocumentName() {
                    return null;
                }

                @Override
                public String getDocumentTitle() {
                    return null;
                }

                @Override
                public String getSenderOrgnumber() {
                    return null;
                }

                @Override
                public String getConversationId() {
                    return null;
                }

                @Override
                public String getPostkasseAdresse() {
                    return null;
                }

                @Override
                public byte[] getCertificate() {
                    return new byte[0];
                }

                @Override
                public String getOrgnrPostkasse() {
                    return null;
                }

                @Override
                public String getSpraakKode() {
                    return null;
                }

                @Override
                public String getQueueId() {
                    return null;
                }

                @Override
                public String getMimeType() {
                    return null;
                }
            });
        } catch (MeldingsformidlerException e) {
            //TODO
        }
//        client.sendMelding();
        return null;
    }
}
