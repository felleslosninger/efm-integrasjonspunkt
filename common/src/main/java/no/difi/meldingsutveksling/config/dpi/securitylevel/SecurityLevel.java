package no.difi.meldingsutveksling.config.dpi.securitylevel;

import no.difi.meldingsutveksling.config.dpi.EnumMapping;
import no.difi.sdp.client2.domain.digital_post.Sikkerhetsnivaa;

import static no.difi.sdp.client2.domain.digital_post.Sikkerhetsnivaa.NIVAA_3;
import static no.difi.sdp.client2.domain.digital_post.Sikkerhetsnivaa.NIVAA_4;

public enum SecurityLevel implements EnumMapping<Sikkerhetsnivaa> {
    LEVEL_3(NIVAA_3), LEVEL_4(NIVAA_4), INVALID;

    private final Sikkerhetsnivaa sikkerhetsnivaa;

    SecurityLevel(Sikkerhetsnivaa sikkerhetsnivaa) {
        this.sikkerhetsnivaa = sikkerhetsnivaa;
    }
    SecurityLevel() {
        /* only used for INVALID */
        sikkerhetsnivaa = null;
    }

    @Override
    public Sikkerhetsnivaa toExternal() {
        return sikkerhetsnivaa;
    }

    public static SecurityLevel fromExternal(Sikkerhetsnivaa sikkerhetsnivaa) {
        for (int i=0; i<values().length; i++) {
            if (values()[i].sikkerhetsnivaa.equals(sikkerhetsnivaa)) {
                return values()[i];
            }
        }
        return null;
    }
}
