package no.difi.meldingsutveksling.ptp;

import no.difi.meldingsutveksling.receipt.ExternalReceipt;
import no.difi.sdp.client2.domain.Prioritet;

import static no.difi.meldingsutveksling.ptp.MeldingsformidlerClientMain.SIKKERHETSNIVAA;
import static no.difi.meldingsutveksling.ptp.MeldingsformidlerClientMain.URL_TESTMILJO;

public class MeldingsformidlerClientSjekkKvitteringMain {
    private static final String SPRAAK_KODE = "NO";
    private static final Prioritet PRIORITET = Prioritet.NORMAL;

    public static void main(String[] args) throws MeldingsformidlerException {
        String mpcId = "1";
        final MeldingsformidlerClient client = new MeldingsformidlerClient(new MeldingsformidlerClient.Config(URL_TESTMILJO, MeldingsformidlerClientMain.createKeyStore(), MeldingsformidlerClientMain.CLIENT_ALIAS, MeldingsformidlerClientMain.PASSWORD, mpcId, SPRAAK_KODE, PRIORITET, SIKKERHETSNIVAA));
        final ExternalReceipt kvittering = client.sjekkEtterKvittering(MeldingsformidlerClientMain.DIFI_ORGNR);
        kvittering.confirmReceipt();


    }
}
