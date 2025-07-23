package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
import no.digdir.altinn3.broker.model.FileTransferStatusDetailsExt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@RequiredArgsConstructor
public class AltinnInSteps {

    private final static QName _GetAvailableFilesBasicResponse_QNAME = new QName("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "GetAvailableFilesBasicResponse");
    private final static QName _CheckIfAvailableFilesBasicResponse_QNAME = new QName("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "CheckIfAvailableFilesBasicResponse");
    private static final String SOAP_ACTION = "SOAPAction";

    private final AltinnZipFactory altinnZipFactory;
    private final Holder<Message> messageInHolder;
    private final WireMockServer wireMockServer;
    private final XMLMarshaller xmlMarshaller;
    private final SaajSoapMessageFactory messageFactory;

    @Before
    public void before() throws IOException {
        wireMockServer.givenThat(get(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasic.svc?wsdl"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("FIXME")
                        //.withBody(IOUtils.toByteArray(no.difi.meldingsutveksling.altinn.mock.brokerbasic.ObjectFactory.class.getResourceAsStream("/BrokerServiceExternalBasic.wsdl")))
                )
        );

        wireMockServer.givenThat(get(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasicStreamed.svc?wsdl"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("FIXME")
                        //.withBody(IOUtils.toByteArray(no.difi.meldingsutveksling.altinn.mock.brokerstreamed.ObjectFactory.class.getResourceAsStream("/BrokerServiceExternalBasicStreamed.wsdl")))
                )
        );

        wireMockServer.givenThat(post(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasic.svc?wsdl"))
                .withHeader(SOAP_ACTION, containing("InitiateBrokerServiceBasic"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"http://www.altinn.no/services/ServiceEngine/Broker/2015/06\">" +
                                "<soapenv:Header/>" +
                                "<soapenv:Body>" +
                                "<ns:InitiateBrokerServiceBasicResponse>" +
                                "</ns:InitiateBrokerServiceBasicResponse>" +
                                "</soapenv:Body>" +
                                "</soapenv:Envelope>")
                )
        );

        wireMockServer.givenThat(post(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasic.svc?wsdl"))
                .withHeader(SOAP_ACTION, containing("ConfirmDownloadedBasic"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"http://www.altinn.no/services/ServiceEngine/Broker/2015/06\">" +
                                "<soapenv:Header/>" +
                                "<soapenv:Body>" +
                                "<ns:InitiateBrokerServiceBasicResponse>" +
                                "</ns:InitiateBrokerServiceBasicResponse>" +
                                "</soapenv:Body>" +
                                "</soapenv:Envelope>")
                )
        );

        wireMockServer.givenThat(post(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasicStreamed.svc?wsdl"))
                .withHeader(SOAP_ACTION, containing("UploadFileStreamedBasic"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE)
                        .withBody("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"http://www.altinn.no/services/ServiceEngine/Broker/2015/06\">" +
                                "<soapenv:Header/>" +
                                "<soapenv:Body>" +
                                "<ns:ReceiptExternalStreamedBE>" +
                                "<!--Optional:-->" +
                                "<ns:LastChanged>?</ns:LastChanged>" +
                                "<!--Optional:-->" +
                                "<ns:ParentReceiptId>?</ns:ParentReceiptId>" +
                                "<!--Optional:-->" +
                                "<ns:ReceiptHistory>?</ns:ReceiptHistory>" +
                                "<!--Optional:-->" +
                                "<ns:ReceiptId>?</ns:ReceiptId>" +
                                "<!--Optional:-->" +
                                "<ns:ReceiptStatusCode>?</ns:ReceiptStatusCode>" +
                                "<!--Optional:-->" +
                                "<ns:ReceiptText>?</ns:ReceiptText>" +
                                "<!--Optional:-->" +
                                "<ns:ReceiptTypeName>?</ns:ReceiptTypeName>" +
                                "</ns:ReceiptExternalStreamedBE>" +
                                "</soapenv:Body>" +
                                "</soapenv:Envelope>")
                )
        );
    }

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^Altinn sends the message$")
    public void altinnSendsTheMessage() throws IOException {

        // OpenAPI / Swagger : https://docs.altinn.studio/nb/api/broker/spec/

        // en fil klar for nedlasting
        UUID fileTransferId  = UUID.randomUUID();
        wireMockServer.givenThat(get(urlEqualTo("/broker/api/v1/filetransfer"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        ["%s"]""".formatted(fileTransferId))
            ));

        // detaljer om filen FileTransferStatusDetailsExt (kunne vært FileTransferStatusExt)
        FileTransferStatusDetailsExt statusDetails = new FileTransferStatusDetailsExt();
        statusDetails.setFileTransferId(fileTransferId);
        statusDetails.setSendersFileTransferReference("SendersReference");
        ObjectMapper om = new ObjectMapper();
        var response = om.writeValueAsString(statusDetails);
        wireMockServer.givenThat(get(urlEqualTo("/broker/api/v1/filetransfer?status=Published&recipientStatus=Initialized"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(response)));

        // download file
        var downloadUrl = "/broker/api/v1/filetransfer/%s/download".formatted(fileTransferId);
        ByteArrayResource altinnZip = altinnZipFactory.createAltinnZip(messageInHolder.get());
        wireMockServer.givenThat(get(urlEqualTo(downloadUrl))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .withBody(altinnZip.getByteArray())
            ));

    }

//    private byte[] getDownloadBody(String boundary) throws IOException {
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        try (PrintWriter pw = new PrintWriter(bos)) {
//            pw.println("--" + boundary);
//            pw.println("Content-ID: <http://tempuri.org/0>");
//            pw.println("Content-Transfer-Encoding: 8bit");
//            pw.println("Content-Type: application/xop+xml;charset=utf-8;type=\"text/xml\"");
//            pw.println();
//            pw.println("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><DownloadFileStreamedBasicResponse xmlns=\"http://www.altinn.no/services/ServiceEngine/Broker/2015/06\"><DownloadFileStreamedBasicResult><xop:Include href=\"cid:http://tempuri.org/1/636854854482615129\" xmlns:xop=\"http://www.w3.org/2004/08/xop/include\"/></DownloadFileStreamedBasicResult></DownloadFileStreamedBasicResponse></s:Body></s:Envelope>");
//            pw.println();
//            pw.println("--" + boundary);
//            pw.println("Content-ID: <http://tempuri.org/1/636854854482615129>");
//            pw.println("Content-Transfer-Encoding: binary");
//            pw.println("Content-Type: application/octet-stream");
//            pw.println();
//            pw.flush();
//            ByteArrayResource altinnZip = altinnZipFactory.createAltinnZip(messageInHolder.get());
//            ResourceUtils.copy(altinnZip, bos);
//            bos.flush();
//            pw.println();
//            pw.println("--" + boundary);
//            pw.flush();
//        }
//
//        return bos.toByteArray();
//    }
//
//    @SneakyThrows
//    private <T> String serialize(JAXBElement<T> jaxbElement) {
//        SaajSoapMessage saajSoapMessage = messageFactory.createWebServiceMessage();
//        SOAPMessage soapMessage = saajSoapMessage.getSaajMessage();
//        SOAPBody body = soapMessage.getSOAPBody();
//        body.addDocument(xmlMarshaller.getDocument(jaxbElement));
//        soapMessage.saveChanges();
//        return toString(soapMessage);
//    }
//
//    private String toString(SOAPMessage soapMessage) throws SOAPException, IOException {
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        soapMessage.saveChanges();
//        soapMessage.writeTo(out);
//        return out.toString();
//    }

}
