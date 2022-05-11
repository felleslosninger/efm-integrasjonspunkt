package no.difi.meldingsutveksling.dpi.xmlsoap.forsendelse;

import no.difi.meldingsutveksling.nextmove.PostalCategory;
import no.difi.meldingsutveksling.nextmove.PrintColor;
import no.difi.meldingsutveksling.nextmove.ReturnHandling;
import no.difi.sdp.client2.domain.fysisk_post.Posttype;
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;

class SDPEnumUtil {

    private SDPEnumUtil() {
        // Utility class
    }

    public static Returhaandtering getReturhaandtering(ReturnHandling returnHandling) {
        switch (returnHandling) {
            case DIREKTE_RETUR:
                return Returhaandtering.DIREKTE_RETUR;
            case MAKULERING_MED_MELDING:
                return Returhaandtering.MAKULERING_MED_MELDING;
            default:
                return null;
        }
    }

    public static Posttype getPosttype(PostalCategory postalCategory) {
        switch (postalCategory) {
            case A_PRIORITERT:
                return Posttype.A_PRIORITERT;
            case B_OEKONOMI:
                return Posttype.B_OEKONOMI;
            default:
                return null;
        }
    }

    public static Utskriftsfarge getUtskriftsfarge(PrintColor printColor) {
        switch (printColor) {
            case SORT_HVIT:
                return Utskriftsfarge.SORT_HVIT;
            case FARGE:
                return Utskriftsfarge.FARGE;
            default:
                return null;
        }
    }
}
