package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by eko on 19.04.2016.
 */
//@Ignore
//public class P360ClientTest {
//    @Mock
//    private NoarkClientSettings noarkClientSettingsMock;
//    @Mock
//    private WebServiceTemplateFactory webServiceTemplateFactoryMock;
//
//    @Mock
//    private WebServiceTemplate webServiceTemplateMaock;

    //@InjectMocks
    //private P360Client p360Client = new P360Client(noarkClientSettingsMock, webServiceTemplateFactoryMock);

//    @Ignore @Before
//    public void setUp() {
//        initMocks(this);
//        when(noarkClientSettingsMock.getEndpointUrl()).thenReturn("http://someendpointurl.no");
//    }


//    @Ignore("Quickfix for KFV testen kommer...")
//    public void shouldPutMessageOnQueueWhenOrganisationNumberIsConfigured() throws Exception {
//
//        when(webServiceTemplateFactoryMock.createTemplate(anyString())).thenReturn(webServiceTemplateMaock);
//        when(webServiceTemplateMaock.sendAndReceive(anyString(), anyObject(), new SoapActionCallback(""))).thenReturn(new Object());
//
//        PutMessageRequestType requestType = new PutMessageRequestType();
//
//        p360Client.sendEduMelding(requestType);
//    }
//}
