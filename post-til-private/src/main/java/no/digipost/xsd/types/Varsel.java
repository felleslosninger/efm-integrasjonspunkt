
package no.digipost.xsd.types;

import no.difi.begrep.sdp.schema_v10.SDPRepetisjoner;

public interface Varsel {

	TekstMedSpraak getVarslingsTekst();

    SDPRepetisjoner getRepetisjoner();

}
