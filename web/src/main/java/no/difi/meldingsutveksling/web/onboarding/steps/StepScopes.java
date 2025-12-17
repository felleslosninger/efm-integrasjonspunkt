package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StepScopes implements Step {

    private boolean STEP_COMPLETED = false;

    @Inject
    FrontendFunctionality ff;

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
        // sjekk om vi får tak i systemuser token - i såfall er dette steget komplett
        STEP_COMPLETED = !STEP_COMPLETED;
    }

    @Override
    public StepInfo getStepInfo() {

        var missingOnboardingScopes = List.of(
            "altinn:authentication/systemregister.write",
            "altinn:authentication/systemuser.request.write",
            "altinn:authentication/systemuser.request.read"
        );

        var missingDpoServiceScopes = List.of(
            "eformidling:dpo",
            "altinn:broker.read",
            "altinn:broker.write"
        );

        var dialogText = STEP_COMPLETED ? """
            Alt i orden, maskinporten-klienten har de nødvendige scopes.""" : """
            Din maskinporten klient <code>%s</code> mangler nødvendige Altinn-scopes for å kunne fullføre
            onboarding-prosessen.<br><br><small><code>%s</code></small><br><br>
            Du mangler også scopes for å kunne benytte DPO broker tjenesten.<br><br>
            <small><code>%s</code></small><br><br>
            Kontakt servicedesk for å få dette fikset.""".formatted(
                getClientId(),
                String.join("<br>", missingOnboardingScopes),
                String.join("<br>", missingDpoServiceScopes)
        );

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

    private String getClientId() {
        return ff.dpoClientId();
    }

}
