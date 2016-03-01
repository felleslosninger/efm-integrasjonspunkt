package no.difi.meldingsutveksling.noarkexchange;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporary fix for FIKS oranisations. Demo on the 3rd of march 2016
 */
@Deprecated
public class FiksFix {

    private static List<String> orgNumberstoreplaceWithKS = new ArrayList<>();

    static {
        orgNumberstoreplaceWithKS.add("910951688");
    }

    public static String replaceOrgNummberWithKs(String orgNumber) {
        if (orgNumberstoreplaceWithKS.contains(orgNumber)) {
            return "910639870";
        } else {
            return orgNumber;
        }
    }
}