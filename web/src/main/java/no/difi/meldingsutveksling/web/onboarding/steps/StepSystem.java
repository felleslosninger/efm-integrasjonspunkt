package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StepSystem implements Step {

    private final String REQUIRED_ACCESS_PACKAGE = "urn:altinn:accesspackage:informasjon-og-kommunikasjon";

    private boolean STEP_COMPLETED = false;
    private boolean systemExists = false;
    private boolean missingRequiredAccessPackage = true;
    private List<String> accessPackages;

    @Inject
    FrontendFunctionality ff;

    @Override
    public String getName() {
        return "system";
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

        // confirm action means verify should try to create the system if one doesn't exist yet
        if (ActionType.CONFIRM.equals(action) && !systemExists) {
            systemExists = ff.dpoCreateSystem(getSystemName(), REQUIRED_ACCESS_PACKAGE);
            missingRequiredAccessPackage = !systemExists;
            STEP_COMPLETED = systemExists;
        }

        // if no system was created, check if one already exists with correct access packages
        if (!STEP_COMPLETED) {
            updateSystemAndAccessPackagesStatus();
            STEP_COMPLETED = systemExists && !missingRequiredAccessPackage;
        }

    }

    @Override
    public StepInfo getStepInfo() {

        executeAction(ActionType.VERIFY);

        var systemName = getSystemName();

        var dialogTextExists = """
            Systemet <code>'%s'</code> er registrert i Altinn's System Register med
            følgende tilgangspakker :<br><br><small><code>%s</code></small><br><br>"""
            .formatted(systemName, String.join("<br>", accessPackages));

        var dialogTextMissingSystem = """
            Vi finner ikke system <code>'%s'</code> i Altinn's System Register.  Sjekk at du har konfigurert
            systemnavn rett i properties filen eller bekreft for å opprette et systemet nå.<br><br>Når dette er
            gjort må du konfigurere rett systemnavnet i properties filen og restarte Integrasjonspunktet."""
            .formatted(systemName);

        var dialogTextMissingAccessPackage = """
            Systemet <code>'%s'</code> finnes i Altinn's System Register, men det mangler tilgangspakken<br><br>
            <code>'%s'</code>.<br><br>Dette må rettes opp før du kan benytte DPO løsningen.  Ditt system
            er registrert med følgende tilgangspakker :<br><br><small><code>%s</code></small><br><br>"""
            .formatted(systemName, REQUIRED_ACCESS_PACKAGE, String.join("<br>", accessPackages));

        var dialog = STEP_COMPLETED ? dialogTextExists : dialogTextMissingSystem;
        if (systemExists && missingRequiredAccessPackage) dialog = dialogTextMissingAccessPackage;

        return new StepInfo(
                getName(),
                "Opprett system",
                "Registrer ett system for Integrasionspunktet i Altinn's System Register, med nødvendige tilgangspakke.",
                dialog,
                isCompleted() ? "Lukk" : "Opprett system",
                isRequired(),
                isCompleted()
        );

    }

    private String getSystemName() {
        return ff.dpoConfiguration().stream()
            .filter(p -> "difi.move.dpo.systemName".equals(p.key()))
            .map(p -> p.value())
            .findFirst()
            .orElse("%s_integrasjonspunkt".formatted(ff.getOrganizationNumber()));
    }

    private void updateSystemAndAccessPackagesStatus() {
        accessPackages = ff.dpoSystemAccessPackages(getSystemName());
        if (accessPackages == null) {
            systemExists = false;
            missingRequiredAccessPackage = true;
            accessPackages = List.of();
        } else {
            systemExists = true;
            missingRequiredAccessPackage = !accessPackages.contains(REQUIRED_ACCESS_PACKAGE);
        }
    }

}
