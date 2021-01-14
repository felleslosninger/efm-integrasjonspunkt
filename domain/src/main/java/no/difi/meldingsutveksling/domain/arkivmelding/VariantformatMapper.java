package no.difi.meldingsutveksling.domain.arkivmelding;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.Variantformat;
import no.difi.meldingsutveksling.HashBiMap;

import static com.google.common.base.Strings.isNullOrEmpty;

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
        if (vf == null || !mapper.containsValue(vf)) {
            log.warn("Variantformat \"{}\" not found in map, defaulting to \"{}\"", vf != null ? vf.value() : "<none>", Variantformat.PRODUKSJONSFORMAT.value());
            return mapper.inverse().get(Variantformat.PRODUKSJONSFORMAT);
        }
        return mapper.inverse().get(vf);
    }

    public static Variantformat getArkivmeldingType(String veVariant) {
        if (isNullOrEmpty(veVariant) || !mapper.containsKey(veVariant)) {
            log.warn("Variantformat \"{}\" not found in map, defaulting to \"{}\"", veVariant, Variantformat.PRODUKSJONSFORMAT.value());
            return Variantformat.PRODUKSJONSFORMAT;
        }
        return mapper.get(veVariant);
    }
}