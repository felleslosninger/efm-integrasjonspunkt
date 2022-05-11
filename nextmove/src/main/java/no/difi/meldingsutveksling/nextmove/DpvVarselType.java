package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DpvVarselType {
    VARSEL_DPV_UTEN_REVARSEL("VarselDPVUtenRevarsel"),
    VARSEL_DPV_MED_REVARSEL("VarselDPVMedRevarsel");

    @JsonValue
    private final String fullname;
}
