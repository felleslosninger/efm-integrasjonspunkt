package no.difi.meldingsutveksling.noarkexchange.cangetmessage;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.services.Adresseregister;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class KanMottaMeldingerSteps {

    private IntegrasjonspunktImpl integrasjonspunkt;
    private Adresseregister adresseRegister;
    private GetCanReceiveMessageResponseType responseType;

    private String message = "&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;\n"
            + "                &lt;Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n"
            + "                xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "                xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"&gt;\n"
            + "                &lt;journpost xmlns=\"\"&gt;\n"
            + "                &lt;jpId&gt;210707&lt;/jpId&gt;\n"
            + "                &lt;jpJaar&gt;2015&lt;/jpJaar&gt;\n"
            + "                &lt;jpSeknr&gt;47&lt;/jpSeknr&gt;\n"
            + "                &lt;jpJpostnr&gt;7&lt;/jpJpostnr&gt;\n"
            + "                &lt;jpJdato&gt;0001-01-01&lt;/jpJdato&gt;\n"
            + "                &lt;jpNdoktype&gt;U&lt;/jpNdoktype&gt;\n"
            + "                &lt;jpDokdato&gt;2015-09-11&lt;/jpDokdato&gt;\n"
            + "                &lt;jpStatus&gt;R&lt;/jpStatus&gt;\n"
            + "                &lt;jpInnhold&gt;Testdokument 7&lt;/jpInnhold&gt;\n"
            + "                &lt;jpForfdato /&gt;\n"
            + "                &lt;jpTgkode&gt;U&lt;/jpTgkode&gt;\n"
            + "                &lt;jpAgdato /&gt;\n"
            + "                &lt;jpAntved /&gt;\n"
            + "                &lt;jpSaar&gt;2015&lt;/jpSaar&gt;\n"
            + "                &lt;jpSaseknr&gt;20&lt;/jpSaseknr&gt;\n"
            + "                &lt;jpOffinnhold&gt;Testdokument 7&lt;/jpOffinnhold&gt;\n"
            + "                &lt;jpTggruppnavn&gt;Alle&lt;/jpTggruppnavn&gt;\n"
            + "                &lt;avsmot&gt;\n"
            + "                &lt;amIhtype&gt;0&lt;/amIhtype&gt;\n"
            + "                &lt;amNavn&gt;Saksbehandler Testbruker7&lt;/amNavn&gt;\n"
            + "                &lt;amAdresse&gt;Postboks 8115 Dep.&lt;/amAdresse&gt;\n"
            + "                &lt;amPostnr&gt;0032&lt;/amPostnr&gt;\n"
            + "                &lt;amPoststed&gt;OSLO&lt;/amPoststed&gt;\n"
            + "                &lt;amUtland&gt;Norge&lt;/amUtland&gt;\n"
            + "                &lt;amEpostadr&gt;sa-user.test2@difi.no&lt;/amEpostadr&gt;\n"
            + "                &lt;/avsmot&gt;\n"
            + "                &lt;avsmot&gt;\n"
            + "                &lt;amOrgnr&gt;974720760&lt;/amOrgnr&gt;\n"
            + "                &lt;amIhtype&gt;1&lt;/amIhtype&gt;\n"
            + "                &lt;amNavn&gt;EduTestOrg 1&lt;/amNavn&gt;\n"
            + "                &lt;/avsmot&gt;\n"
            + "                &lt;dokument&gt;\n"
            + "                &lt;dlRnr&gt;1&lt;/dlRnr&gt;\n"
            + "                &lt;dlType&gt;H&lt;/dlType&gt;\n"
            + "                &lt;dbTittel&gt;Testdokument 7&lt;/dbTittel&gt;\n"
            + "                &lt;dbStatus&gt;B&lt;/dbStatus&gt;\n"
            + "                &lt;veVariant&gt;P&lt;/veVariant&gt;\n"
            + "                &lt;veDokformat&gt;DOCX&lt;/veDokformat&gt;\n"
            + "                &lt;fil&gt;\n"
            + ""
            + "                &lt;/fil&gt;\n"
            + "                &lt;veFilnavn&gt;Testdokument 7.DOCX&lt;/veFilnavn&gt;\n"
            + "                &lt;veMimeType&gt;application/vnd.openxmlformats-officedocument.wordprocessingml.document&lt;/veMimeType&gt;\n"
            + "                &lt;/dokument&gt;\n"
            + "                &lt;/journpost&gt;\n"
            + "                &lt;noarksak xmlns=\"\"&gt;\n"
            + "                &lt;saId&gt;15/00020&lt;/saId&gt;\n"
            + "                &lt;saSaar&gt;2015&lt;/saSaar&gt;\n"
            + "                &lt;saSeknr&gt;20&lt;/saSeknr&gt;\n"
            + "                &lt;saPapir&gt;0&lt;/saPapir&gt;\n"
            + "                &lt;saDato&gt;2015-09-01&lt;/saDato&gt;\n"
            + "                &lt;saTittel&gt;BEST/EDU testsak&lt;/saTittel&gt;\n"
            + "                &lt;saStatus&gt;R&lt;/saStatus&gt;\n"
            + "                &lt;saArkdel&gt;Sakarkiv 2013&lt;/saArkdel&gt;\n"
            + "                &lt;saType&gt;Sak&lt;/saType&gt;\n"
            + "                &lt;saJenhet&gt;Oslo&lt;/saJenhet&gt;\n"
            + "                &lt;saTgkode&gt;U&lt;/saTgkode&gt;\n"
            + "                &lt;saBevtid /&gt;\n"
            + "                &lt;saKasskode&gt;B&lt;/saKasskode&gt;\n"
            + "                &lt;saOfftittel&gt;BEST/EDU testsak&lt;/saOfftittel&gt;\n"
            + "                &lt;saAdmkort&gt;202286&lt;/saAdmkort&gt;\n"
            + "                &lt;saAdmbet&gt;Seksjon for test 1&lt;/saAdmbet&gt;\n"
            + "                &lt;saAnsvinit&gt;difi\\sa-user-test2&lt;/saAnsvinit&gt;\n"
            + "                &lt;saAnsvnavn&gt;Saksbehandler Testbruker7&lt;/saAnsvnavn&gt;\n"
            + "                &lt;saTggruppnavn&gt;Alle&lt;/saTggruppnavn&gt;\n"
            + "                &lt;sakspart&gt;\n"
            + "                &lt;spId&gt;0&lt;/spId&gt;\n"
            + "                &lt;/sakspart&gt;\n"
            + "                &lt;/noarksak&gt;\n"
            + "                &lt;/Melding&gt;";

    @Before
    public void setup() {
        integrasjonspunkt = new IntegrasjonspunktImpl();
        adresseRegister = mock(Adresseregister.class);
    }

    @Given("^virksomhet (.+) i Adresseregisteret$")
    public void virksomhet_i_Adresseregisteret(String finnes) throws Throwable {
        if ("finnes".equals(finnes)) {
            X509Certificate certificate = mock(X509Certificate.class);
            when(adresseRegister.getCertificate(any(ServiceRecord.class))).thenReturn(certificate);
        } else if ("finnes ikke".equals(finnes)) {
            when(adresseRegister.getCertificate(any(ServiceRecord.class))).thenThrow(CertificateException.class);
        }
    }

    @When("^vi spør integrasjonspunktet om virksomhet kan motta meldinger$")
    public void vi_spØr_integrasjonspunktet_om_virksomhet_kan_motta_meldinger() throws Throwable {
        // Express the Regexp above with the code you wish you had
        GetCanReceiveMessageRequestType request = new GetCanReceiveMessageRequestType();
        AddressType addressType = new AddressType();
        addressType.setOrgnr("12345678");
        request.setReceiver(addressType);
        responseType = integrasjonspunkt.getCanReceiveMessage(request);
    }

    @Then("^skal vi få (.+) om at de kan motta meldinger$")
    public void skal_vi_få_svar_om_at_de_kan_motta_meldinger(String svar) throws Throwable {
        if ("sann".equals(svar)) {
            assertTrue(responseType.isResult());
        } else if ("usann".equals(svar)) {
            assertFalse(responseType.isResult());
        }

    }

    @When("^integrasjonspunktet mottar en melding$")
    public void integrasjonspunktet_mottar_en_melding() throws Throwable {
        // Express the Regexp above with the code you wish you had
        PutMessageRequestType req = new PutMessageRequestType();
        EnvelopeType envelope = new EnvelopeType();
        AddressType addressType = new AddressType();
        addressType.setOrgnr("12345678");
        envelope.setReceiver(addressType);
        AddressType sender = new AddressType();
        sender.setOrgnr("87654321");
        envelope.setSender(sender);
        req.setEnvelope(envelope);
        req.setPayload(message);
        integrasjonspunkt.putMessage(req);

    }

}
