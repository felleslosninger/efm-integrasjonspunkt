package no.difi.meldingsutveksling.domain.arkivmelding;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.beta.Avskrivningsmaate;
import no.difi.meldingsutveksling.HashBiMap;

@Slf4j
public class AvskrivningsmaateMapper {

    private static final HashBiMap<String, Avskrivningsmaate> mapper = new HashBiMap<>();

    private AvskrivningsmaateMapper() {
    }

    static {
        mapper.put("BU", Avskrivningsmaate.BESVART_MED_BREV);
        // TODO: potential wrong mapping
        mapper.put("NN", Avskrivningsmaate.BESVART_MED_E_POST);
        mapper.put("TLF", Avskrivningsmaate.BESVART_PÅ_TELEFON);
        mapper.put("TE", Avskrivningsmaate.TATT_TIL_ETTERRETNING);
        mapper.put("TO", Avskrivningsmaate.TATT_TIL_ORIENTERING);
    }

    public static String getNoarkType(Avskrivningsmaate am) {
        if (!mapper.containsValue(am)) {
            log.warn("Avskrivningsmaate \"{}\" not found in map, defaulting to \"{}\"", am.value(), Avskrivningsmaate.BESVART_MED_BREV.value());
            return mapper.inverse().get(Avskrivningsmaate.BESVART_MED_BREV);
        }
        return mapper.inverse().get(am);
    }

    public static Avskrivningsmaate getArkivmeldingType(String amAvskm) {
        if (!mapper.containsKey(amAvskm)) {
            log.warn("Avskrivningsmaate \"{}\" not found in map, defaulting to \"{}\"", amAvskm, Avskrivningsmaate.BESVART_MED_BREV.value());
            return Avskrivningsmaate.BESVART_MED_BREV;
        }
        return mapper.get(amAvskm);
    }
}