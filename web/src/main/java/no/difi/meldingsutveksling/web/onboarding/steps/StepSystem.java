package no.difi.meldingsutveksling.web.onboarding.steps;

import org.springframework.stereotype.Service;

@Service
public class StepSystem implements Step {

    private boolean STEP_COMPLETED = false;

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

        var dialogText = STEP_COMPLETED ?
            "Systemet 'yyyy' er registrert i Altinn's ressurs-register." :
            "Oppretter system 'yyyy' i Altinn's ressurs-register.<br><br>Når det er gjort må du konfigurere det i properties filen og restarte Integrasjonspunktet.";

        return new StepInfo(
                getName(),
                "Opprett system",
                "Registrer ett system i Altinn's ressurs-register for Integrasjonspunktet.",
                dialogText,
                isCompleted() ? "Lukk" : "Opprett system",
                isRequired(),
                isCompleted()
        );

    }

}

