package no.difi.meldingsutveksling.config.dpi;

import no.difi.sdp.client2.domain.fysisk_post.Posttype;

public enum ShippingType implements EnumMapping<Posttype> {
    PRIORITY(Posttype.A_PRIORITERT), ECONOMY(Posttype.B_OEKONOMI);

    private Posttype postType;

    ShippingType(Posttype postType) {
        this.postType = postType;
    }

    @Override
    public Posttype toExternal() {
        return postType;
    }

    public static ShippingType fromExternal(Posttype posttype) {
        for (int i=0; i<values().length; i++) {
            if (values()[i].postType.equals(posttype)) {
                return values()[i];
            }
        }
        return null;
    }
}
