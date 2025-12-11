package no.difi.meldingsutveksling.web.onboarding.steps;

import org.springframework.stereotype.Service;

@Service
public class StepElma implements Step {

    private boolean STEP_COMPLETED = false;

    @Override
    public String getName() {
        return "elma";
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public boolean isCompleted() {
        return STEP_COMPLETED;
    }

    @Override
    public void verify(String value) {
        STEP_COMPLETED = !STEP_COMPLETED;
    }

    @Override
    public StepInfo getStepInfo() {

        var dialogText = STEP_COMPLETED ?
            "Du er registert som mottaker i ELMA." :
            "Virksomhet 'zzzz' er ikke registrert som mottaker av arkiv- eller avtalt-meldinger i ELMA.<br><br>Kontakt servicedesk for å få dette fikset.";

        return new StepInfo(
                getName(),
                "ELMA-registrering",
                "Registrer organisasjonen og dokumenttypene i ELMA slik at meldingene kan rutes.",
                dialogText,
                "Lukk",
                isRequired(),
                isCompleted()
        );

    }

}
