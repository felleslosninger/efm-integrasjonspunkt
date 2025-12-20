package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

@Service
public class StepKonfigurer implements Step {

    private boolean STEP_COMPLETED = false;

    @Inject
    FrontendFunctionality ff;

    @Override
    public String getName() {
        return "konfigurer";
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
            "Konfigurasjon ser ut til å være i orden." :  """
            Du mangler disse konfigurasjonene.<br><br><small><code>
            difi.move.dpo.systemName=%s<br>
            difi.move.dpo.systemUser.orgId=%s<br>
            difi.move.dpo.systemUser.name=%s<br>
            </code></small>
            <br>
            Restart integrasjonspunktet når du har gjort endringene.""".formatted(getSystemName(), getSystemOrgId(), getSystemUserName());

        return new StepInfo(
                getName(),
                "Konfigurer Integrasjonspunktet",
                "Sørg for at konfigurasjon av Integrasjonspunktet inneholder systemnavn og systembrukerene du har opprettet.",
                dialogText,
                "Lukk",
                isRequired(),
                isCompleted()
        );

    }

    private String getSystemName() {
        // 311780735_integrasjonspunkt
        return ff.dpoConfiguration().stream()
            .filter(p -> "difi.move.dpo.systemName".equals(p.key()))
            .map(p -> p.value())
            .findFirst()
            .orElse("%s_integrasjonspunkt".formatted(ff.getOrganizationNumber()));
    }

    private String getSystemOrgId() {
        // 0192:311780735
        return ff.dpoConfiguration().stream()
            .filter(p -> "difi.move.dpo.systemUser.orgId".equals(p.key()))
            .map(p -> p.value())
            .findFirst()
            .orElse("0192:%s".formatted(ff.getOrganizationNumber()));
    }

    private String getSystemUserName() {
        // 311780735_integrasjonspunkt_systembruker_test3
        return ff.dpoConfiguration().stream()
            .filter(p -> "difi.move.dpo.systemUser.name".equals(p.key()))
            .map(p -> p.value())
            .findFirst()
            .orElse("%s_systembruker_%s".formatted(getSystemName(), ff.getOrganizationNumber()));
    }

}
