package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import no.difi.meldingsutveksling.web.FrontendFunctionality.Property;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StepPaavegneav implements Step {

    private final String REQUIRED_ACCESS_PACKAGE = "urn:altinn:accesspackage:informasjon-og-kommunikasjon";

    @Inject
    FrontendFunctionality ff;

    enum StepState {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        ERROR
    }

    private StepState state = StepState.NOT_STARTED;

    // info about the system we are registering on
    private String systemName;
    private String errorMessage;

    // info about the new organization and system user we are adding
    private String orgNumber;
    private String systemUserName;
    private String acceptSystemUserURL;

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

        if (state == StepState.ERROR) {
            // something is wrong with expected configuration, we don't allow any actions
            return;
        }

        if (action == ActionType.CANCEL) {
            // we will only cancel if we are in progress
            if (StepState.IN_PROGRESS.equals(state)) state = StepState.NOT_STARTED;
            return;
        }

        if (state == StepState.NOT_STARTED) {
            if (orgNumber == null) orgNumber = "";
            if (orgNumber.length() == 9) {
                systemUserName = systemName + "_systembruker_" + orgNumber;
                errorMessage = "";
                state = StepState.IN_PROGRESS;
            } else {
                errorMessage = "Må være 9 siffer";
            }
        } else if (state == StepState.IN_PROGRESS) {
            acceptSystemUserURL = ff.dpoCreateSystemUser(systemUserName, systemName, orgNumber, REQUIRED_ACCESS_PACKAGE);
            if (acceptSystemUserURL != null) {
                state = StepState.COMPLETED;
            } else {
                errorMessage = "Kunne ikke opprette systembruker, sjekk logger for feilmelding.";
                state = StepState.ERROR;
            }
        } else if (state == StepState.COMPLETED) {
            orgNumber = "";
            state = StepState.NOT_STARTED;
        }

    }

    @Override
    public StepInfo getStepInfo() {

        if (systemName == null) systemName = getSystemName();
        if (errorMessage == null) errorMessage = "";
        if (orgNumber == null) orgNumber = "";

        var dialogText = "Systemleverandører som skal sende og motta meldinger på vegne av andre virksomheter kan registrere flere systembrukere i Altinn her.";

        var dialogTextSystembrukere = "Finner ingen systembrukere for system <code>%s</code>.<br><br>".formatted(systemName);

        var systemUsers = ff.dpoSystemUsersForSystem(systemName);

        if (!systemUsers.isEmpty()) dialogTextSystembrukere = """
             På system <code>'%s'</code> er følgende systembruker(e) allerede registrert :<br><br>
             <small><code>%s</code></small><br><br>"""
            .formatted(systemName, String.join("<br>", systemUsers));

        // update dialogs for each state

        if (state == StepState.NOT_STARTED) {
            var dialogOrgInputFields = """
                <label for="paavegneav-orgnummer">Orgnummer på ny virksomhet du vil registrere</label><br>
                <input type="text" id="paavegneav-orgnummer" name="orgnummer" maxlength="9" pattern="[0-9]{9}" placeholder="9 siffer" value="%s">
                <small><code>%s</code></small><br>
                """.formatted(orgNumber, errorMessage);
            dialogText = dialogText + "<br><br>" + dialogTextSystembrukere + dialogOrgInputFields;
        }

        if (state == StepState.IN_PROGRESS) {
            var dialogSystemUserInputFields = """
                <label for="paavegneav-systemuser">Navn på systembruker som opprettes for %s vil bli :</label><br>
                <input type="text" id="paavegneav-systemuser" name="systemuser" value="%s" style="width:100%%">
                <small><code>%s</code></small><br>
                """.formatted(orgNumber, systemUserName, errorMessage);
            dialogText = dialogText + "<br><br>" + dialogTextSystembrukere + dialogSystemUserInputFields;
        }

        if (state == StepState.COMPLETED) {
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

        if (state == StepState.ERROR) {
            dialogText = """
                Ikke mulig å opprette systembruker - sjekk at du har konfigurert system innstillinger korrekt.<br><br>
                <small><code>System Name = %s</code></small><br>
                Siste feilmelding var :<br>
                <small><code>%s</code></small>
                """.formatted(systemName, errorMessage);
        }

        //  update button text for each state

        var buttonText = switch (state) {
            case NOT_STARTED -> "Start med orgnummer";
            case IN_PROGRESS -> "Registrer i Altinn";
            case COMPLETED -> "Opprett enda en systembruker";
            case ERROR -> "Kan ikke utføre noe";
        };

        // return state with dialog text, flags and button text to ui

        return new StepInfo(
            getName(),
            "På vegne av systembrukere",
            "Systemleverandørere kan registrere systembrukere i Altinn for andre organisasjoner og virksomheter de skal sende og motta meldinger på vegne av.",
            dialogText,
            buttonText,
            isRequired(),
            isCompleted(),
            state != StepState.ERROR,
            state != StepState.ERROR,
            true
        );

    }

    private String getSystemName() {
        return ff.dpoConfiguration().stream()
            .filter(p -> "difi.move.dpo.systemName".equals(p.key()))
            .map(Property::value)
            .findFirst()
            .orElse("difi.move.dpo.systemName er ikke konfigurert");
    }

}
