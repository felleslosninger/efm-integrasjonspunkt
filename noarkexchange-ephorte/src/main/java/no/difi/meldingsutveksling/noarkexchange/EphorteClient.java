package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.ephorte.schema.NoarkExchange;
import no.difi.meldingsutveksling.noarkexchange.ephorte.schema.NoarkExchangeBinding;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import javax.xml.ws.BindingProvider;
import java.util.Map;

public class EphorteClient implements NoarkClient {
    private NoarkClientSettings settings;
    private final NoarkExchange noarkExchange;

    public EphorteClient(NoarkClientSettings settings) {
        noarkExchange = new NoarkExchange();
        NoarkExchangeBinding binding = noarkExchange.getNoarkExchangeBinding();
        BindingProvider bindingProvider = (BindingProvider) binding;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, this.settings.getEndpointUrl());
        requestContext.put(BindingProvider.USERNAME_PROPERTY, this.settings.getUserName());
        requestContext.put(BindingProvider.PASSWORD_PROPERTY, this.settings.getPassword());
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        return new PutMessageResponseType();
    }
}
