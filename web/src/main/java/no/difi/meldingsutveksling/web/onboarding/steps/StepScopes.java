package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StepScopes implements Step {

    private boolean STEP_COMPLETED = false;

    private boolean completedOnboardingScopes = false;
    private boolean completedServicesScopes = false;
    private boolean completedEformidlingScopes = false;

    @Inject
    FrontendFunctionality ff;

    private static List<String> allOnboardingScopes = List.of(
        "altinn:authentication/systemregister.write",
        "altinn:authentication/systemuser.request.write",
        "altinn:authentication/systemuser.request.read"
    );

    private static List<String> allServicesScopes = List.of(
        "altinn:broker.read",
        "altinn:broker.write"
    );

    private static List<String> allEformidlingScopes = List.of(
        "eformidling:dpo"
    );

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

        if (STEP_COMPLETED) return;

        completedOnboardingScopes = (ff.dpoAccessToken(allOnboardingScopes) != null);
        completedServicesScopes = (ff.dpoAccessToken(allServicesScopes) != null);
        completedEformidlingScopes = (ff.dpoAccessToken(allEformidlingScopes) != null);

        STEP_COMPLETED = completedOnboardingScopes && completedServicesScopes && completedEformidlingScopes;

    }

    @Override
    public StepInfo getStepInfo() {

        verify("verify_scopes");

        if (STEP_COMPLETED) {
            return new StepInfo(
                getName(),
                "Tildel scopes",
                "Din maskinporten-klienten har de nødvendige scopes.",
                "Alt i orden, din maskinporten-klienten har alle nødvendige scopes",
                "Lukk",
                isRequired(),
                isCompleted()
            );
        }

        var dialogText = """
            Din maskinporten klient <code>%s</code> mangler nødvendige scopes for å kunne
            ta i bruk DPO tjenesten til eformidling.<br><br>""".formatted(getClientId());

        if (!completedOnboardingScopes) dialogText += """
            Du mangler Altinn onboarding scopes, for å registrer deg som bruker av DPO.<br><br>
            <small><code>%s</code></small><br><br>
            """.formatted(String.join("<br>", allOnboardingScopes));

        if (!completedServicesScopes) dialogText += """
            Du mangler scopes for å kunne sende og motta via DPO broker tjenesten i Altinn 3.<br><br>
            <small><code>%s</code></small><br><br>
            """.formatted(String.join("<br>", allServicesScopes));

        if (!completedEformidlingScopes) dialogText += """
            Du mangler scope for å benytte DPO tjenesten til eFormidling.<br><br>
            <small><code>%s</code></small><br><br>
            """.formatted(String.join("<br>", allEformidlingScopes));

        dialogText += "Kontakt servicedesk for å få dette fikset.";

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
