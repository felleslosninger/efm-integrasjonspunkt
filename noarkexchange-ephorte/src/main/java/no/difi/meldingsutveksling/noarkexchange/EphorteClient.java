package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.ephorte.schema.NoarkExchange;
import no.difi.meldingsutveksling.noarkexchange.ephorte.schema.NoarkExchangeBinding;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import javax.xml.ws.BindingProvider;
import java.util.Map;

public class EphorteClient implements NoarkClient {
    private final NoarkExchange noarkExchange;

    public EphorteClient(NoarkClientSettings settings) {
        noarkExchange = new NoarkExchange();
        NoarkExchangeBinding binding = noarkExchange.getNoarkExchangeBinding();
        BindingProvider bindingProvider = (BindingProvider) binding;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, settings.getEndpointUrl());
        requestContext.put(BindingProvider.USERNAME_PROPERTY, settings.getUserName());
        requestContext.put(BindingProvider.PASSWORD_PROPERTY, settings.getPassword());
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        return new PutMessageResponseType();
    }
}
