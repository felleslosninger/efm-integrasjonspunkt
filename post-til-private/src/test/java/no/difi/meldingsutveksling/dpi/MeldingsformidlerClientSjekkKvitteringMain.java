package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.receipt.ExternalReceipt;
import no.difi.sdp.client2.domain.Prioritet;

import static no.difi.meldingsutveksling.dpi.MeldingsformidlerClientMain.createKeyStore;
import static no.difi.meldingsutveksling.dpi.MeldingsformidlerClientMain.getDigitalPostInnbyggerConfig;

public class MeldingsformidlerClientSjekkKvitteringMain {
    private static final String SPRAAK_KODE = "NO";
    private static final Prioritet PRIORITET = Prioritet.NORMAL;


    public static final boolean ENABLE_EMAIL = false;
    public static final boolean ENABLE_SMS = false;

    public static void main(String[] args) throws MeldingsformidlerException {
        String mpcId = "1";
        DigitalPostInnbyggerConfig config = getDigitalPostInnbyggerConfig(mpcId);
        SikkerDigitalPostKlientFactory sikkerDigitalPostKlientFactory = new SikkerDigitalPostKlientFactory(config, createKeyStore());
        ForsendelseHandlerFactory forsendelseHandlerFactory = new ForsendelseHandlerFactory(config);
        MeldingsformidlerClient client = new MeldingsformidlerClient(config, sikkerDigitalPostKlientFactory, forsendelseHandlerFactory);

        final ExternalReceipt kvittering = client.sjekkEtterKvittering(MeldingsformidlerClientMain.DIFI_ORGNR);
        kvittering.confirmReceipt();
    }
}
