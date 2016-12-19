package no.difi.meldingsutveksling.config.dpi;

import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;

public enum InkType implements EnumMapping<Utskriftsfarge> {
    BLACK_WHITE(Utskriftsfarge.SORT_HVIT), COLOUR(Utskriftsfarge.FARGE);

    private Utskriftsfarge utskriftsfarge;

    InkType(Utskriftsfarge utskriftsfarge) {
        this.utskriftsfarge = utskriftsfarge;
    }

    @Override
    public Utskriftsfarge toExternal() {
        return utskriftsfarge;
    }
}
