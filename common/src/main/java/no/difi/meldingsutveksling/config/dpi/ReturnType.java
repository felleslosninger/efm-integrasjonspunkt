package no.difi.meldingsutveksling.config.dpi;

import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;

public enum ReturnType implements EnumMapping<Returhaandtering> {
    SHREDDING(Returhaandtering.MAKULERING_MED_MELDING),
    DIRECT_RETURN(Returhaandtering.DIREKTE_RETUR);

    private final Returhaandtering returhaandtering;

    ReturnType(Returhaandtering returhaandtering) {
        this.returhaandtering = returhaandtering;
    }

    @Override
    public Returhaandtering toExternal() {
        return returhaandtering;
    }

    public static ReturnType fromExternal(Returhaandtering returhaandtering) {
        for (int i=0; i<values().length; i++) {
            if (values()[i].returhaandtering.equals(returhaandtering)) {
                return values()[i];
            }
        }
        return null;
    }
}
