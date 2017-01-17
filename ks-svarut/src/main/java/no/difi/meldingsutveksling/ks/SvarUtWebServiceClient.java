package no.difi.meldingsutveksling.ks;

/**
 * Created by mfhoel on 15.12.2016.
 */
public interface SvarUtWebServiceClient {
    String sendMessage(Forsendelse forsendelse);

    ForsendelseStatus getForsendelseStatus(String forsendesId);
}
