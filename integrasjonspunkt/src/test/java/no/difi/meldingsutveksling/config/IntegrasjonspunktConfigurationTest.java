package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRequiredPropertyException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;

import static no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration.KEY_ADRESSEREGISTER_ENDPOINT;
import static no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration.KEY_KEYSTORE_LOCATION;
import static no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration.KEY_NOARKSYSTEM_ENDPOINT;
import static no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration.KEY_PRIVATEKEYALIAS;
import static no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration.KEY_PRIVATEKEYPASSWORD;
import static no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration.NOARKSYSTEM_TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class IntegrasjonspunktConfigurationTest {
    IntegrasjonspunktConfiguration configuration;

    @Mock
    Environment environmentMock;

    @Before
    public void setUp() {
        initMocks(this);

        //Default with values set on required properties
        when(environmentMock.getProperty(KEY_NOARKSYSTEM_ENDPOINT)).thenReturn("something");
        when(environmentMock.getProperty(KEY_ADRESSEREGISTER_ENDPOINT)).thenReturn("something");
        when(environmentMock.getProperty(KEY_PRIVATEKEYALIAS)).thenReturn("something");
        when(environmentMock.getProperty(KEY_KEYSTORE_LOCATION)).thenReturn("something");
        when(environmentMock.getProperty(KEY_PRIVATEKEYPASSWORD)).thenReturn("something");
        when(environmentMock.getProperty(KEY_ORGANISATION_NUMBER)).thenReturn("something");
        when(environmentMock.getProperty(KEY_NOARKSYSTEM_TYPE)).thenReturn("something");
    }

    @Test(expected = MeldingsUtvekslingRequiredPropertyException.class)
    public void shouldGetExceptionWhenNoarkSystemIsBlank() throws Exception {
        when(environmentMock.getProperty(KEY_NOARKSYSTEM_ENDPOINT)).thenReturn("");

        configuration = new IntegrasjonspunktConfiguration(environmentMock);
    }

    @Test(expected = MeldingsUtvekslingRequiredPropertyException.class)
    public void shouldGetExceptionWhenAdresseregisterEndpointIsBlank() throws Exception {
        when(environmentMock.getProperty(KEY_ADRESSEREGISTER_ENDPOINT)).thenReturn("");

        configuration = new IntegrasjonspunktConfiguration(environmentMock);
    }

    @Test(expected = MeldingsUtvekslingRequiredPropertyException.class)
    public void shouldGetExceptionWhenPrivatekeyaliasIsBlank() throws Exception {
        when(environmentMock.getProperty(KEY_PRIVATEKEYALIAS)).thenReturn("");

        configuration = new IntegrasjonspunktConfiguration(environmentMock);
    }

    @Test(expected = MeldingsUtvekslingRequiredPropertyException.class)
    public void shouldGetExceptionWhenKeystorelocationIsBlank() throws Exception {
        when(environmentMock.getProperty(KEY_KEYSTORE_LOCATION)).thenReturn(null);

        configuration = new IntegrasjonspunktConfiguration(environmentMock);
    }

    @Test(expected = MeldingsUtvekslingRequiredPropertyException.class)
    public void shouldGetExceptionWhenPrivatekeypasswordIsBlank() throws Exception {
        when(environmentMock.getProperty(KEY_PRIVATEKEYPASSWORD)).thenReturn(null);

        configuration = new IntegrasjonspunktConfiguration(environmentMock);
    }

    @Test(expected = MeldingsUtvekslingRequiredPropertyException.class)
    public void shouldGetExceptionWhenNoarksystemtypeIsBlank() throws Exception {
        when(environmentMock.getProperty(KEY_NOARKSYSTEM_TYPE)).thenReturn(null);

        configuration = new IntegrasjonspunktConfiguration(environmentMock);
    }

    @Test
    public void shouldNotFailInitializationWhenAllRequiredParametersIsSet() throws Exception{
        //Default values on properties is set in @Before
        configuration = new IntegrasjonspunktConfiguration(environmentMock);
    }
}