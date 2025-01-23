package no.digipost.xsd.types;

import no.difi.begrep.sdp.schema_v10.SDPEpostVarsel;
import no.difi.begrep.sdp.schema_v10.SDPSmsVarsel;
import no.difi.begrep.sdp.schema_v10.SDPVarsler;

import java.util.Optional;

public interface HarVarsler {

    SDPVarsler getVarsler();

    default Optional<SDPSmsVarsel> getSmsVarsel() {
        return Optional.ofNullable(getVarsler()).map(SDPVarsler::getSmsVarsel);
    }

    default Optional<SDPEpostVarsel> getEpostVarsel() {
        return Optional.ofNullable(getVarsler()).map(SDPVarsler::getEpostVarsel);
    }
}
