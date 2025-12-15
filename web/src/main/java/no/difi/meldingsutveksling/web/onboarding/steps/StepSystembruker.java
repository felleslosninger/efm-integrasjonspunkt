package no.difi.meldingsutveksling.web.onboarding.steps;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Service;

@Service
public class StepSystembruker implements Step {

    private boolean STEP_COMPLETED = false;

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
    public void verify(String value) {
        STEP_COMPLETED = !STEP_COMPLETED;
    }

    @Override
    public StepInfo getStepInfo() {

        var dialogText = STEP_COMPLETED ? """
            Systembruker <code>'%s'</code> er registrert på system <code>'%s'</code>.""".formatted(getSystemUserName(), getSystemName()) : """
            Vi finner ikke systembruker <code>'%s'</code> i Altinn's ressurs-register.  Sjekk at du har konfigurert
            systembruker rett i properties filen eller bekreft for å opprette en systembruker nå.<br><br>
            Om du bekrefter vil det opprettes en systembruker for virksomhet <code>'%s'</code>
            på system <code>'%s'</code>.<br><br> Husk at ansvarlig for virksomhet <code>'%s'</code> må bekrefte
            opprettelsen av systembruker i Altinn før den blir aktivert og DPO tjenesten kan tas i bruk.<br><br>
            Systembrukeren som opprettes vil få navn <code>'%s'</code>.<br><br>
            Når dette er gjort må du konfigurere det i properties filen og restarte Integrasjonspunktet."""
            .formatted(getSystemUserName(), ff.getOrganizationNumber(), getSystemName(), ff.getOrganizationNumber(), getSystemUserName());

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

    private String getSystemName() {
        // 311780735_integrasjonspunkt
        return ff.configurationDPO().stream()
            .filter(p -> "difi.move.dpo.systemName".equals(p.key()))
            .map(p -> p.value())
            .findFirst()
            .orElse("%s_integrasjonspunkt".formatted(ff.getOrganizationNumber()));
    }

    private String getSystemOrgId() {
        // 0192:311780735
        return ff.configurationDPO().stream()
            .filter(p -> "difi.move.dpo.systemUser.orgId".equals(p.key()))
            .map(p -> p.value())
            .findFirst()
            .orElse("0192:%s".formatted(ff.getOrganizationNumber()));
    }

    private String getSystemUserName() {
        // 311780735_integrasjonspunkt_systembruker_test3
        return ff.configurationDPO().stream()
            .filter(p -> "difi.move.dpo.systemUser.name".equals(p.key()))
            .map(p -> p.value())
            .findFirst()
            .orElse("%s_systembruker_%s".formatted(getSystemName(), ff.getOrganizationNumber()));
    }

}
