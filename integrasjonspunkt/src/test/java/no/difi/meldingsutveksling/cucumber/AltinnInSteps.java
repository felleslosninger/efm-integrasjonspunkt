package no.difi.meldingsutveksling.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.*;
import no.difi.move.common.io.ResourceUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
                        .withBody(IOUtils.toByteArray(no.difi.meldingsutveksling.altinn.mock.brokerbasic.ObjectFactory.class.getResourceAsStream("/BrokerServiceExternalBasic.wsdl")))
                )
        );

        wireMockServer.givenThat(get(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasicStreamed.svc?wsdl"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(IOUtils.toByteArray(no.difi.meldingsutveksling.altinn.mock.brokerstreamed.ObjectFactory.class.getResourceAsStream("/BrokerServiceExternalBasicStreamed.wsdl")))
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
        BrokerServiceAvailableFileList filesBasic = new BrokerServiceAvailableFileList();
        BrokerServiceAvailableFile file = new BrokerServiceAvailableFile();
        file.setFileReference("testMessage");
        file.setSendersReference(new ObjectFactory().createBrokerServiceAvailableFileSendersReference(UUID.randomUUID().toString()));
        file.setReceiptID(1);
        filesBasic.getBrokerServiceAvailableFile().add(file);

        CheckIfAvailableFilesBasicResponse checkResponse = new ObjectFactory().createCheckIfAvailableFilesBasicResponse();
        checkResponse.setCheckIfAvailableFilesBasicResult(true);
        wireMockServer.givenThat(post(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasic.svc?wsdl"))
                .withHeader(SOAP_ACTION, containing("CheckIfAvailableFilesBasic"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONNECTION, "close")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE)
                        .withBody(serialize(new JAXBElement<>(_CheckIfAvailableFilesBasicResponse_QNAME, CheckIfAvailableFilesBasicResponse.class, checkResponse)))
                )
        );

        GetAvailableFilesBasicResponse response = new no.difi.meldingsutveksling.altinn.mock.brokerbasic.ObjectFactory().createGetAvailableFilesBasicResponse();
        response.setGetAvailableFilesBasicResult(new no.difi.meldingsutveksling.altinn.mock.brokerbasic.ObjectFactory().createGetAvailableFilesBasicResponseGetAvailableFilesBasicResult(filesBasic));
        wireMockServer.givenThat(post(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasic.svc?wsdl"))
                .withHeader(SOAP_ACTION, containing("GetAvailableFilesBasic"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONNECTION, "close")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE)
                        .withBody(serialize(new JAXBElement<>(_GetAvailableFilesBasicResponse_QNAME, GetAvailableFilesBasicResponse.class, response)))
                )
        );

        String boundary = UUID.randomUUID().toString();

        wireMockServer.givenThat(post(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasicStreamed.svc?wsdl"))
                        .withHeader(SOAP_ACTION, containing("DownloadFileStreamedBasic"))
                        .willReturn(aResponse()
                                        .withStatus(200)
//                        .withHeader(HttpHeaders.CONNECTION, "close")
                                        .withHeader(HttpHeaders.CACHE_CONTROL, "private")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, String.format("multipart/related; type=\"application/xop+xml\";start=\"<http://tempuri.org/0>\";boundary=\"%s\";start-info=\"text/xml\"", boundary))
                                        .withHeader("MIME-Version", "1.0")
                                        .withHeader(HttpHeaders.SERVER, "Microsoft-IIS/8.5")
                                        .withHeader(HttpHeaders.TRANSFER_ENCODING, "chunked")
                                        .withHeader("X-AspNet-Version", "4.0.30319")
                                        .withHeader("X-Powered-By", "ASP.NET")
                                        .withBody(getDownloadBody(boundary))
                        )
        );
    }

    private byte[] getDownloadBody(String boundary) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(bos)) {
            pw.println("--" + boundary);
            pw.println("Content-ID: <http://tempuri.org/0>");
            pw.println("Content-Transfer-Encoding: 8bit");
            pw.println("Content-Type: application/xop+xml;charset=utf-8;type=\"text/xml\"");
            pw.println();
            pw.println("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><DownloadFileStreamedBasicResponse xmlns=\"http://www.altinn.no/services/ServiceEngine/Broker/2015/06\"><DownloadFileStreamedBasicResult><xop:Include href=\"cid:http://tempuri.org/1/636854854482615129\" xmlns:xop=\"http://www.w3.org/2004/08/xop/include\"/></DownloadFileStreamedBasicResult></DownloadFileStreamedBasicResponse></s:Body></s:Envelope>");
            pw.println();
            pw.println("--" + boundary);
            pw.println("Content-ID: <http://tempuri.org/1/636854854482615129>");
            pw.println("Content-Transfer-Encoding: binary");
            pw.println("Content-Type: application/octet-stream");
            pw.println();
            pw.flush();
            ByteArrayResource altinnZip = altinnZipFactory.createAltinnZip(messageInHolder.get());
            ResourceUtils.copy(altinnZip, bos);
            bos.flush();
            pw.println();
            pw.println("--" + boundary);
            pw.flush();
        }

        return bos.toByteArray();
    }

    @SneakyThrows
    private <T> String serialize(JAXBElement<T> jaxbElement) {
        SaajSoapMessage saajSoapMessage = messageFactory.createWebServiceMessage();
        SOAPMessage soapMessage = saajSoapMessage.getSaajMessage();
        SOAPBody body = soapMessage.getSOAPBody();
        body.addDocument(xmlMarshaller.getDocument(jaxbElement));
        soapMessage.saveChanges();
        return toString(soapMessage);
    }

    private String toString(SOAPMessage soapMessage) throws SOAPException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        soapMessage.saveChanges();
        soapMessage.writeTo(out);
        return out.toString();
    }
}
