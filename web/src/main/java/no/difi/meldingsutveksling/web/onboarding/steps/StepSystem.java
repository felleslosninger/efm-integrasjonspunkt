package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StepSystem implements Step {

    private boolean STEP_COMPLETED = false;

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
    public void verify(String value) {

        if (STEP_COMPLETED) return;

        // confirm action means verify should try to create the system
        if ("confirm".equalsIgnoreCase(value)) {
            STEP_COMPLETED = ff.dpoCreateSystem(getSystemName());
        }

        // if no system was created, check if one already exists
        if (!STEP_COMPLETED) {
            var details = ff.dpoSystemAccessPackages();
            STEP_COMPLETED = details != null;
        }

    }

    @Override
    public StepInfo getStepInfo() {

        verify("verify_step");

        var dialogTextExists = """
            Systemet <code>'%s'</code> er registrert i Altinn's System Register med
            følgende tilgangspakker :<br><br><small><code>%s</code></small><br><br>"""
            .formatted(getSystemName(), String.join("<br>", getAccessPackagesForSystem()));

        var dialogTextMissingSystem = """
            Vi finner ikke system <code>'%s'</code> i Altinn's System Register.  Sjekk at du har konfigurert
            systemnavn rett i properties filen eller bekreft for å opprette et systemet nå.<br><br>Når dette er
            gjort må du konfigurere rett systemnavnet i properties filen og restarte Integrasjonspunktet."""
            .formatted(getSystemName());

        return new StepInfo(
                getName(),
                "Opprett system",
                "Registrer ett system for Integrasionspunktet i Altinn's System Register, med nødvendige tilgangspakke.",
                STEP_COMPLETED ? dialogTextExists : dialogTextMissingSystem,
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

    private List<String> getAccessPackagesForSystem() {
        return ff.dpoSystemAccessPackages();
    }

}
