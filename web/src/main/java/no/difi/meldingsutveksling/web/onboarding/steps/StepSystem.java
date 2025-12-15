package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

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
        STEP_COMPLETED = !STEP_COMPLETED;
    }

    @Override
    public StepInfo getStepInfo() {

        var dialogText = STEP_COMPLETED ? """
            Systemet <code>'%s'</code> er registrert i Altinn's ressurs-register.""" : """
            Vi finner ikke system <code>'%s'</code> i Altinn's ressurs-register.  Sjekk at du har konfigurert
            systemnavn rett i properties filen eller bekreft for å opprette et systemet nå.<br><br>Når dette er
            gjort må du konfigurere rett systemnavnet i properties filen og restarte Integrasjonspunktet.""";

        return new StepInfo(
                getName(),
                "Opprett system",
                "Registrer ett system i Altinn's ressurs-register for Integrasjonspunktet.",
                dialogText.formatted(getSystemName()),
                isCompleted() ? "Lukk" : "Opprett system",
                isRequired(),
                isCompleted()
        );

    }

    private String getSystemName() {
        return ff.configurationDPO().stream()
            .filter(p -> "difi.move.dpo.systemName".equals(p.key()))
            .map(p -> p.value())
            .findFirst()
            .orElse("%s_integrasjonspunkt".formatted(ff.getOrganizationNumber()));
    }

}

