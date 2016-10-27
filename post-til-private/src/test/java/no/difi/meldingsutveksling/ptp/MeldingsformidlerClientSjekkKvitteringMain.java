package no.difi.meldingsutveksling.ptp;

import static no.difi.meldingsutveksling.ptp.MeldingsformidlerClientMain.URL_TESTMILJO;

public class MeldingsformidlerClientSjekkKvitteringMain {
    public static void main(String[] args) throws MeldingsformidlerException {
        String mpcId = "1";
        final MeldingsformidlerClient client = new MeldingsformidlerClient(new MeldingsformidlerClient.Config(URL_TESTMILJO, MeldingsformidlerClientMain.createKeyStore(), MeldingsformidlerClientMain.CLIENT_ALIAS, MeldingsformidlerClientMain.PASSWORD, mpcId));
        final MeldingsformidlerClient.Kvittering kvittering = client.sjekkEtterKvittering(MeldingsformidlerClientMain.DIFI_ORGNR);

        kvittering.executeCallback();


    }
}
