package no.difi.meldingsutveksling.domain.arkivmelding;

import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.apache.commons.io.FileUtils;
import org.eclipse.persistence.internal.oxm.ByteArraySource;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.FileUtils.getFile;

public class ArkivmeldingFactoryTest {

    IntegrasjonspunktProperties properties;
    ArkivmeldingFactory arkivmeldingFactory;
    Validator validator;
    ArkivmeldingUtil arkivmeldingUtil;
    Unmarshaller unmarshaller;

    public ArkivmeldingFactoryTest() throws SAXException, JAXBException {
        properties = new IntegrasjonspunktProperties();
        properties.setNoarkSystem(new IntegrasjonspunktProperties.NorskArkivstandardSystem());
        properties.getNoarkSystem().setType("P360");

        arkivmeldingFactory = new ArkivmeldingFactory(properties);

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(getFile("../nextmove/src/main/resources/xsd/arkivmelding.xsd"));
        Schema schema = factory.newSchema(schemaFile);
        validator = schema.newValidator();

        arkivmeldingUtil = new ArkivmeldingUtil();

        JAXBContext putMessageJaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
        unmarshaller = putMessageJaxbContext.createUnmarshaller();
    }

    @Test
    public void test() throws SAXException, IOException, JAXBException {
        byte[] payload = FileUtils.readFileToByteArray(new File("../integrasjonspunkt/src/test/resources/putmessage_test.xml"));
        PutMessageRequestType pm = unmarshaller.unmarshal(new ByteArraySource(payload), PutMessageRequestType.class).getValue();;
        PutMessageRequestWrapper putMessage = new PutMessageRequestWrapper(pm);

        Arkivmelding arkivmelding = arkivmeldingFactory.from(putMessage);
        byte[] arkivmeldingBytes = arkivmeldingUtil.marshalArkivmelding(arkivmelding);
        String s = new String(arkivmeldingBytes);
        System.out.println(s);
        validator.validate(new ByteArraySource(arkivmeldingBytes));
    }
}
