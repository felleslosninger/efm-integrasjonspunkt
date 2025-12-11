package no.difi.meldingsutveksling.web.onboarding.steps;

import org.springframework.stereotype.Service;

@Service
public class StepSystembruker implements Step {

    private boolean STEP_COMPLETED = false;

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
        STEP_COMPLETED = !STEP_COMPLETED;
    }

    @Override
    public StepInfo getStepInfo() {

        var dialogText = STEP_COMPLETED ?
            "Systembruker 'xxxx' er registrert for ditt system 'yyyy'." :
            "Oppretter systembruker 'xxxx' på ditt system 'yyyy'.<br><br>Når det er gjort må du konfigurere det i properties filen og restarte Integrasjonspunktet.";

        return new StepInfo(
                getName(),
                "Opprett systembruker",
                "Registrer systembrukere i Altinn for alle de organisasjoner og virksomheter du vil sende og motta meldinger for.",
                dialogText,
                isCompleted() ? "Lukk" : "Opprett systembruker",
                isRequired(),
                isCompleted()
        );

    }

}
