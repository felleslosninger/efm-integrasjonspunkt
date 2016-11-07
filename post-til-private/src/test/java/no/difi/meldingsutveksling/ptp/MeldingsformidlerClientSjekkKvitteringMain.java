package no.difi.meldingsutveksling.ptp;

import no.difi.meldingsutveksling.receipt.ExternalReceipt;

import static no.difi.meldingsutveksling.ptp.MeldingsformidlerClientMain.URL_TESTMILJO;

public class MeldingsformidlerClientSjekkKvitteringMain {

    public static final boolean ENABLE_EMAIL = false;
    public static final boolean ENABLE_SMS = false;

    public static void main(String[] args) throws MeldingsformidlerException {
        String mpcId = "1";
        final MeldingsformidlerClient client = new MeldingsformidlerClient(new MeldingsformidlerClient.Config(URL_TESTMILJO, MeldingsformidlerClientMain.createKeyStore(), MeldingsformidlerClientMain.CLIENT_ALIAS, MeldingsformidlerClientMain.PASSWORD, mpcId, ENABLE_EMAIL, ENABLE_SMS));
        final ExternalReceipt kvittering = client.sjekkEtterKvittering(MeldingsformidlerClientMain.DIFI_ORGNR);
        kvittering.confirmReceipt();


    }
}
