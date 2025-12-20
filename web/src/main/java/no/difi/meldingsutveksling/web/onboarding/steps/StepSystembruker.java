package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

@Service
public class StepSystembruker implements Step {

    private boolean STEP_COMPLETED = false;

    @Inject
    FrontendFunctionality ff;

    @Override
    public String getName() {
        return "systemuser";
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

        if ("confirm".equalsIgnoreCase(value)) {
            // FIXME opprett systembruker, husk at denne ikke er aktiv før noen har godkjent den
        }

        var details = ff.dpoSystemUsersForSystem();
        STEP_COMPLETED = details != null;

    }

    @Override
    public StepInfo getStepInfo() {

        var dialogTextFinished = """
            Systembruker <code>'%s'</code> er registrert på system <code>'%s'</code>."""
            .formatted(getSystemUserName(), getSystemName());

        var dialogTextMissing = "Vi finner ikke systembruker <code>'%s'</code> i Altinn's System Register.<br><br>"
            .formatted(getSystemUserName());

        if (!ff.dpoSystemUsersForSystem().isEmpty()) dialogTextMissing = dialogTextMissing + """
             Men på system <code>'%s'</code> er følgende systembrukere allerede er registrert :<br><br>
             <small><code>%s</code></small><br><br>"""
            .formatted(getSystemName(), String.join("<br>", ff.dpoSystemUsersForSystem()));

        dialogTextMissing = dialogTextMissing + """
            Sjekk at du har konfigurert systembruker rett i properties filen eller bekreft at du vil å opprette
            en ny systembruker.<br><br>Om du bekrefter vil det opprettes en systembruker for
            virksomhet <code>'%s'</code> på system <code>'%s'</code>.<br><br>
            Husk at ansvarlig for virksomhet <code>'%s'</code> må bekrefte opprettelsen av systembruker i Altinn
            før den blir aktivert og DPO tjenesten kan tas i bruk.<br><br>
            Systembrukeren som opprettes vil få navn <code>'%s'</code>.<br><br>
            Når dette er gjort må du konfigurere om properties filen og restarte Integrasjonspunktet."""
            .formatted(getSystemOrgId(), getSystemName(), getOrgNumberFromOrgId(), getSystemUserName());

        return new StepInfo(
                getName(),
                "Opprett systembruker",
                "Registrer systembrukere i Altinn for alle de organisasjoner og virksomheter du vil sende og motta meldinger for.",
                STEP_COMPLETED ? dialogTextFinished : dialogTextMissing,
                isCompleted() ? "Lukk" : "Opprett systembruker",
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

    private Object getOrgNumberFromOrgId() {
        return getSystemOrgId().split(":")[1];
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
