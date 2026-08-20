package no.difi.meldingsutveksling.ks.svarut.ws;

import org.springframework.ws.client.core.WebServiceTemplate;

public interface SvarUtWebServiceTemplateSupplier {

    WebServiceTemplate getWebServiceTemplate();
}
