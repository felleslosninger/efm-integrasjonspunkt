package no.difi.meldingsutveksling.config.dpi;

import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;

public enum InkType implements EnumMapping<Utskriftsfarge> {
    BLACK_WHITE(Utskriftsfarge.SORT_HVIT),
    COLOUR(Utskriftsfarge.FARGE);

    private Utskriftsfarge utskriftsfarge;

    InkType(Utskriftsfarge utskriftsfarge) {
        this.utskriftsfarge = utskriftsfarge;
    }

    @Override
    public Utskriftsfarge toExternal() {
        return utskriftsfarge;
    }

    public static InkType fromExternal(Utskriftsfarge utskriftsfarge) {
        for (int i=0; i<values().length; i++) {
            if (values()[i].utskriftsfarge.equals(utskriftsfarge)) {
                return values()[i];
            }
        }
        return null;
    }
}
