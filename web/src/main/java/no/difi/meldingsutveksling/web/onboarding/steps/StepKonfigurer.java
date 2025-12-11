package no.difi.meldingsutveksling.web.onboarding.steps;

import org.springframework.stereotype.Service;

@Service
public class StepKonfigurer implements Step {

    private boolean STEP_COMPLETED = false;

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
            "Konfigurasjon ser ut til å være i orden." :
            "Du mangler disse konfigurasjonene.<br><br><code>- xxxxx<br>- yyyyy<br>- zzzzz</code><br><br>Restart integrasjonspunktet når du har gjort endringene.";

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

}
