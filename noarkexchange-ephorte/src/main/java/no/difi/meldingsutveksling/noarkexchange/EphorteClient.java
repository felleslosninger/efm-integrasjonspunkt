package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.ephorte.schema.NoarkExchange;
import no.difi.meldingsutveksling.noarkexchange.ephorte.schema.NoarkExchangeBinding;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import javax.xml.ws.BindingProvider;
import java.util.Map;

public class EphorteClient implements NoarkClient {
    private final NoarkExchangeBinding binding;

    public EphorteClient(NoarkClientSettings settings) {
        NoarkExchange noarkExchange = new NoarkExchange();
        binding = noarkExchange.getNoarkExchangeBinding();
        BindingProvider bindingProvider = (BindingProvider) binding;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, settings.getEndpointUrl());
        requestContext.put(BindingProvider.USERNAME_PROPERTY, settings.getUserName());
        requestContext.put(BindingProvider.PASSWORD_PROPERTY, settings.getPassword());
    }

    @Override
    public GetCanReceiveMessageResponseType canRecieveMessage(GetCanReceiveMessageRequestType request) {
        return null;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        return new PutMessageResponseType();
    }
}
