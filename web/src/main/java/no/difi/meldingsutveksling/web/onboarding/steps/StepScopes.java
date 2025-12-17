package no.difi.meldingsutveksling.web.onboarding.steps;

import org.springframework.stereotype.Service;

@Service
public class StepScopes implements Step {

    private boolean STEP_COMPLETED = false;

    @Override
    public String getName() {
        return "scopes";
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
            "Alt i orden, maskinporten-klienten har nødvendige Altinn-scopes." :
            "Din maskinporten-klient mangler nødvendige Altinn-scopes for å kunne fullføre onboarding-prosessen. Du mangler også scope <code>eformidling:dpo</code> for å kunne benytte tjenesten.<br><br>Kontakt servicedesk for å få dette fikset.";

        return new StepInfo(
                getName(),
                "Tildel scopes",
                "Legg til nødvendige Altinn-scopes på maskinporten-klienten for å kunne kjøre onboarding.",
                dialogText,
                "Lukk",
                isRequired(),
                isCompleted()
        );

    }

}
