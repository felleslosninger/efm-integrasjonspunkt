package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.Forsendelse;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.verify;


@RequiredArgsConstructor
public class DigipostOutSteps {

    private final SikkerDigitalPostKlient sikkerDigitalPostKlient;

    @Before
    public void before() {
    }

    @After
    public void after() {
    }


    @Then("^a message is sent to Digipost$")
    public void anUploadToDigipostIsInitiatedWith() {

        ArgumentCaptor<Forsendelse> forsendelseArgumentCaptor = ArgumentCaptor.forClass(Forsendelse.class);
        verify(sikkerDigitalPostKlient).send(forsendelseArgumentCaptor.capture());

        Forsendelse forsendelse = forsendelseArgumentCaptor.getValue();


    }
}
