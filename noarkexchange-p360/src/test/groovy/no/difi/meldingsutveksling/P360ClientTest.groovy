package no.difi.meldingsutveksling

import net.logstash.logback.marker.LogstashMarker
import no.difi.meldingsutveksling.noarkexchange.NoarkClientSettings
import no.difi.meldingsutveksling.noarkexchange.P360Client
import no.difi.meldingsutveksling.noarkexchange.WebServiceTemplateFactory
import no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageResponseType as P360PutMessageResponse
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.springframework.ws.client.core.WebServiceMessageCallback
import org.springframework.ws.client.core.WebServiceTemplate

import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

@RunWith(JUnit4)
class P360ClientTest {

    @Mock
    WebServiceTemplate webServiceTemplate
    @Mock
    WebServiceTemplateFactory webServiceTemplateFactory
    @Mock
    NoarkClientSettings noarkClientSettings

    P360Client p360Client


    @Before
    void setup() {
        initMocks(this)
        when(noarkClientSettings.createTemplateFactory()).thenReturn(webServiceTemplateFactory)
        when(webServiceTemplateFactory.createTemplate(any(), any(LogstashMarker))).thenReturn(webServiceTemplate)
        p360Client = new P360Client(noarkClientSettings)
    }

    @Test
    void testSendEduMelding() {
        when(webServiceTemplate.marshalSendAndReceive(any(), any(), any(WebServiceMessageCallback.class)))
                .thenReturn(new JAXBElement<>(new QName(""), P360PutMessageResponse, new P360PutMessageResponse()))

        PutMessageResponseType responseType = p360Client.sendEduMelding(new PutMessageRequestType(payload: "", envelope: new EnvelopeType(receiver: new AddressType(orgnr: "1234"))))

        assert responseType?.result?.type == "OK"
    }
}
