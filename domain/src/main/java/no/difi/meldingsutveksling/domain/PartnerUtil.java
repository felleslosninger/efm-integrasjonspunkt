package no.difi.meldingsutveksling.domain;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PartnerUtil {

    public static String getPartOrPrimaryIdentifier(PartnerIdentifier identifier) {
        return identifier.hasOrganizationPartIdentifier() ?
                identifier.getOrganizationPartIdentifier() :
                identifier.getIdentifier();
    }

}
