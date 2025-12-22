package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

@Service
public class StepSystembruker implements Step {

    private final String REQUIRED_ACCESS_PACKAGE = "urn:altinn:accesspackage:informasjon-og-kommunikasjon";

    private boolean STEP_COMPLETED = false;
    private String acceptSystemUserURL = null;

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
    public void executeAction(ActionType action) {

        if (STEP_COMPLETED) return;

        // confirm action means verify should try to create a system user
        if (ActionType.CONFIRM.equals(action)) {
            acceptSystemUserURL = ff.dpoCreateSystemUser(getSystemUserName(), getSystemName(), getOrgNumberFromOrgId(), REQUIRED_ACCESS_PACKAGE);
            if (acceptSystemUserURL != null) STEP_COMPLETED = true;
        }

        // if no system user has been created, check if the configured one already exists
        if (!STEP_COMPLETED) {
            STEP_COMPLETED = ff.dpoSystemUserExists(getSystemName(), getSystemUserName());
        }

    }

    @Override
    public StepInfo getStepInfo() {

        executeAction(ActionType.VERIFY);

        final var systemName = getSystemName();
        final var systemUserName = getSystemUserName();

        var dialogCreatedButNotConfirmed = """
            Opprettelse av systembruker <code>'%s'</code> er registrert på system <code>'%s'</code>, men må
            godkjennes før det kan benyttes.<br><br>En ansvarlig for bedriften må logge inn på Altinn med
            sin nettleser og bekreftet at det opprettes en systembruker for virksomheten.<br><br>
            Du kan videreformidle godkjennings URL'en nedenfor til vedkommende for å komme direkte til
            godkjenningen det gjelder :<br><br><code>%s</code>
            """
            .formatted(systemUserName, systemName, acceptSystemUserURL);

        var dialogTextFinished = """
            Systembruker <code>'%s'</code> er registrert på system <code>'%s'</code>."""
            .formatted(systemUserName, systemName);

        var dialogTextMissing = "Vi finner ikke systembruker <code>'%s'</code> i Altinn's System Register.<br><br>"
            .formatted(systemUserName);

        if (!ff.dpoSystemUsersForSystem(systemName).isEmpty()) dialogTextMissing = dialogTextMissing + """
             Men på system <code>'%s'</code> er følgende systembrukere allerede er registrert :<br><br>
             <small><code>%s</code></small><br><br>"""
            .formatted(systemName, String.join("<br>", ff.dpoSystemUsersForSystem(systemName)));

        dialogTextMissing = dialogTextMissing + """
            Sjekk at du har konfigurert systembruker rett i properties filen eller bekreft at du vil å opprette
            en ny systembruker.<br><br>Om du bekrefter vil det opprettes en systembruker for
            virksomhet <code>'%s'</code> på system <code>'%s'</code>.<br><br>
            Husk at ansvarlig for virksomhet <code>'%s'</code> må bekrefte opprettelsen av systembruker i Altinn
            før den blir aktivert og DPO tjenesten kan tas i bruk.<br><br>
            Systembrukeren som opprettes vil få navn <code>'%s'</code>.<br><br>
            Når dette er gjort må du konfigurere om properties filen og restarte Integrasjonspunktet."""
            .formatted(getSystemOrgId(), systemName, getOrgNumberFromOrgId(), systemUserName);

        var dialog = STEP_COMPLETED ? dialogTextFinished : dialogTextMissing;
        if ((!STEP_COMPLETED) && (acceptSystemUserURL != null)) dialog = dialogCreatedButNotConfirmed;

        var buttonText = isCompleted() ? "Lukk" : "Opprett systembruker";
        if (acceptSystemUserURL != null) buttonText = "Godkjenn i Altinn";

        return new StepInfo(
                getName(),
                "Opprett systembruker",
                "Registrer systembrukere i Altinn for alle de organisasjoner og virksomheter du vil sende og motta meldinger for.",
                dialog,
                buttonText,
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

    private String getOrgNumberFromOrgId() {
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
