package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StepPaavegneav implements Step {

    @Inject
    FrontendFunctionality ff;

    enum StepState {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }

    private StepState state = StepState.NOT_STARTED;
    private String orgNumber;
    private String systemName;
    private String systemUserName;
    private String acceptSystemUserURL;
    private String errorMessage;

    @Override
    public String getName() {
        return "paavegneav";
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean isCompleted() {
        return false;
    }

    @Override
    public void setParams(java.util.Map<String, String> params) {
        if (params != null) {
            orgNumber = Optional.ofNullable(params.get("orgnummer")).orElse(orgNumber);
            systemUserName = Optional.ofNullable(params.get("systemuser")).orElse(systemUserName);
        }
    }

    @Override
    public void executeAction(ActionType action) {

        if (StepState.NOT_STARTED.equals(state)) {
            if (orgNumber.length() == 9) {
                systemUserName = systemName + "_" + orgNumber;
                errorMessage = "";
                state = StepState.IN_PROGRESS;
            } else {
                errorMessage = "Må være 9 siffer";
            }
        } else if (StepState.IN_PROGRESS.equals(state)) {
            acceptSystemUserURL = "https://altinn.no/systembruker/" + systemUserName + "/godkjenn";
            state = StepState.COMPLETED;
        } else if (StepState.COMPLETED.equals(state)) {
            orgNumber = "";
            state = StepState.NOT_STARTED;
        }

    }

    @Override
    public StepInfo getStepInfo() {

        if (systemName == null) systemName = getSystemName();
        if (orgNumber == null) orgNumber = "";
        if (errorMessage == null) errorMessage = "";

        var dialogText = "Systemleverandører som skal sende og motta meldinger på vegne av andre virksomheter kan registrere flere systembrukere i Altinn her.";

        var dialogTextSystembrukere = "Finner ingen systembrukere for system <code>%s</code>.<br><br>".formatted(systemName);

        if (!ff.dpoSystemUsersForSystem(systemName).isEmpty()) dialogTextSystembrukere = """
             På system <code>'%s'</code> er følgende systembruker(e) allerede registrert :<br><br>
             <small><code>%s</code></small><br><br>"""
            .formatted(systemName, String.join("<br>", ff.dpoSystemUsersForSystem(systemName)));

        //
        // update dialogs for each state
        //

        if (StepState.NOT_STARTED.equals(state)) {
            var dialogOrgInputFields = """
                <label for="paavegneav-orgnummer">Orgnummer på ny virksomhet du vil registrere</label><br>
                <input type="text" id="paavegneav-orgnummer" name="orgnummer" maxlength="9" pattern="[0-9]{9}" placeholder="9 siffer" value="%s">
                <small><code>%s</code></small><br>
                """.formatted(orgNumber, errorMessage);
            dialogText = dialogText + "<br><br>" + dialogTextSystembrukere + dialogOrgInputFields;
        }

        if (StepState.IN_PROGRESS.equals(state)) {
            var dialogSystemUserInputFields = """
                <label for="paavegneav-systemuser">Navn på systembruker som opprettes for %s vil bli :</label><br>
                <input type="text" id="paavegneav-systemuser" name="systemuser" value="%s" style="width:100%%">
                <small><code>%s</code></small><br>
                """.formatted(orgNumber, systemUserName, errorMessage);
            dialogText = dialogText + "<br><br>" + dialogTextSystembrukere + dialogSystemUserInputFields;
        }

        if (StepState.COMPLETED.equals(state)) {
            var dialogSystemUserCreated = """
                Opprettelse av systembruker <code>'%s'</code> for organisasjon <code>'%s'</code> er registrert
                på system <code>'%s'</code>, men må godkjennes før det kan benyttes.<br><br>
                En ansvarlig for organisasjon <code>'%s'</code> må logge inn på Altinn med sin nettleser og
                bekreftet at det opprettes en systembruker for virksomheten.<br><br>
                Du kan videreformidle godkjennings URL'en nedenfor til ansvarlig i virksomheten
                for å komme direkte til godkjenningen det gjelder :<br><br><code>%s</code><br><br>
                Kopier URL'en ovenfor <b>før du fortsetter</b> med å opprette flere systembrukere.
                """
                .formatted(systemUserName, orgNumber, systemName, orgNumber, acceptSystemUserURL);
            dialogText = dialogSystemUserCreated;
        }

        //
        //  update button text for each state
        //

        var buttonText = switch (state) {
            case NOT_STARTED -> "Start med orgnummer";
            case IN_PROGRESS -> "Opprett systembruker";
            case COMPLETED -> "Registrer ny systembruker";
        };

        //
        // return state with dialog text, flags and button text to ui
        //

        return new StepInfo(
                getName(),
                "På vegne av systembrukere",
                "Systemleverandørere kan registrere systembrukere i Altinn for andre organisasjoner og virksomheter de skal sende og motta meldinger på vegne av.",
                dialogText,
                buttonText,
                isRequired(),
                isCompleted(),
                true,
            true
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

    private String getSystemUserName(String orgnummer) {
        // 311780735_integrasjonspunkt_systembruker_test3
        return ff.dpoConfiguration().stream()
            .filter(p -> "difi.move.dpo.systemUser.name".equals(p.key()))
            .map(p -> p.value())
            .findFirst()
            .orElse("%s_systembruker_%s".formatted(getSystemName(), ff.getOrganizationNumber()));
    }

}
