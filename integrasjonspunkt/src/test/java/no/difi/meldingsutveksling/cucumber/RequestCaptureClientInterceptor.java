package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.TransformerHelper;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class RequestCaptureClientInterceptor extends ClientInterceptorAdapter {

    private final TransformerHelper transformerHelper = new TransformerHelper();
    private final Holder<List<String>> webServicePayloadHolder;

    @Override
    @SneakyThrows(TransformerException.class)
    public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
        Source payloadSource = messageContext.getRequest().getPayloadSource();
        StringResult stringResult = new StringResult();
        transformerHelper.transform(payloadSource, stringResult);
        String payload = stringResult.toString();
        webServicePayloadHolder.getOrCalculate(ArrayList::new)
                .add(payload);
        return true;
    }
}
