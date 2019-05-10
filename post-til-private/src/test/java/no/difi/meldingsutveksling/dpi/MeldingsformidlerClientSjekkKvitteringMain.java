package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.receipt.ExternalReceipt;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.sdp.client2.domain.Prioritet;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

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
        Clock clock = Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), ZoneId.of("Europe/Oslo"));
        MessageStatusFactory messageStatusFactory = new MessageStatusFactory(clock);
        DpiReceiptMapper dpiReceiptMapper = new DpiReceiptMapper(messageStatusFactory, clock);
        MeldingsformidlerClient client = new MeldingsformidlerClient(config, sikkerDigitalPostKlientFactory, forsendelseHandlerFactory,dpiReceiptMapper);
        final ExternalReceipt kvittering = client.sjekkEtterKvittering(MeldingsformidlerClientMain.DIFI_ORGNR);
        kvittering.confirmReceipt();
    }
}
