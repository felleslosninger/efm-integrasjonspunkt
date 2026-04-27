package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

@Service
public class StepPaavegneav implements Step {

    @Inject
    FrontendFunctionality ff;

    private String systemName;
    private String orgnummer;
    private String systemUserName;
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
        if (params != null) orgnummer = params.get("orgnummer");
    }

    @Override
    public void executeAction(ActionType action) {
        System.out.println("Looking for new system user: " + orgnummer);
    }

    @Override
    public StepInfo getStepInfo() {

        var dialogText = "Systemleverandører som skal sende og motta meldinger på vegne av andre virksomheter kan registrere flere systembrukere i Altinn her.";

        if (systemName == null) systemName = getSystemName();
        if (orgnummer == null) orgnummer = "";
        errorMessage = null;

        var dialogTextSystembrukere = "Finner ingen systembrukere for system <code>%s</code>.<br><br>".formatted(systemName);

        if (!ff.dpoSystemUsersForSystem(systemName).isEmpty()) dialogTextSystembrukere = """
             På system <code>'%s'</code> er følgende systembruker(e) allerede registrert :<br><br>
             <small><code>%s</code></small><br><br>"""
            .formatted(systemName, String.join("<br>", ff.dpoSystemUsersForSystem(systemName)));

        if (orgnummer.length() == 9) {
            systemUserName = systemName + "_" + orgnummer;
        } else {
            errorMessage = "Må være 9 siffer";
        }

        if (errorMessage != null) {
            var dialogOrgInputFields = """
                <label for="paavegneav-orgnummer">Orgnummer på ny virksomhet du vil registrere</label><br>
                <input type="text" id="paavegneav-orgnummer" name="orgnummer" maxlength="9" pattern="[0-9]{9}" placeholder="9 siffer" value="%s">
                <small><code>%s</code></small><br>
                """.formatted(orgnummer, errorMessage);
            dialogText = dialogText + "<br><br>" + dialogTextSystembrukere + dialogOrgInputFields;
        } else {
            var dialogSystemUserInputFields = """
                <label for="paavegneav-ss">Navn på systembruker du vil opprette for %s</label><br>
                <input type="text" id="paavegneav-ss" name="ss" value="%s" style="width:100%%">
                """.formatted(orgnummer, systemUserName);
            dialogText = dialogText + "<br><br>" + dialogTextSystembrukere + dialogSystemUserInputFields;
        }

        return new StepInfo(
                getName(),
                "På vegne av systembrukere",
                "Systemleverandørere kan registrere systembrukere i Altinn for andre organisasjoner og virksomheter de skal sende og motta meldinger på vegne av.",
                dialogText,
                "Sjekk på-vegne-av systembruker",
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
