package no.difi.meldingsutveksling.domain.arkivmelding;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.beta.Variantformat;
import no.difi.meldingsutveksling.HashBiMap;

@Slf4j
public class VariantformatMapper {

    private static final HashBiMap<String, Variantformat> mapper = new HashBiMap<>();

    private VariantformatMapper() {
    }

    static {
        mapper.put("P", Variantformat.PRODUKSJONSFORMAT);
        mapper.put("A", Variantformat.ARKIVFORMAT);
        mapper.put("O", Variantformat.DOKUMENT_HVOR_DELER_AV_INNHOLDET_ER_SKJERMET);
    }

    public static String getNoarkType(Variantformat vf) {
        if (!mapper.containsValue(vf)) {
            log.warn("Variantformat \"{}\" not found in map, defaulting to \"{}\"", vf.value(), Variantformat.PRODUKSJONSFORMAT.value());
            return mapper.inverse().get(vf);
        }
        return mapper.inverse().get(vf);
    }

    public static Variantformat getArkivmeldingType(String veVariant) {
        if (!mapper.containsKey(veVariant)) {
            log.warn("Variantformat \"{}\" not found in map, defaulting to \"{}\"", veVariant, Variantformat.PRODUKSJONSFORMAT.value());
            return Variantformat.PRODUKSJONSFORMAT;
        }
        return mapper.get(veVariant);
    }
}