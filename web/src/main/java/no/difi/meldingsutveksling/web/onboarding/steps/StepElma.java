package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

@Service
public class StepElma implements Step {

    private boolean STEP_COMPLETED = false;

    @Inject
    FrontendFunctionality ff;

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

        var dialogText = STEP_COMPLETED ? """
            Du er registert som mottaker i ELMA register med identifikator <code>'0192:%s'</code>.""" : """
            Virksomhet <code>'%s'</code> er <b>ikke</b> registrert som mottaker av arkiv- eller avtalt-meldinger
            i ELMA.<br><br>Kontakt servicedesk for å få dette fikset.""";

        return new StepInfo(
                getName(),
                "ELMA-registrering",
                "Registrer organisasjonen og dokumenttypene i ELMA slik at du kan motta meldinger.",
                dialogText.formatted(ff.getOrganizationNumber()),
                "Lukk",
                isRequired(),
                isCompleted()
        );

    }

}
