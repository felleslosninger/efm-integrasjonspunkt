package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory;
import no.difi.meldingsutveksling.DateTimeUtil;
import org.eclipse.persistence.internal.oxm.ByteArraySource;
import org.xml.sax.SAXException;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringWriter;

import static org.apache.commons.io.FileUtils.getFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SvarInnNextMoveConverterTest {

    String ksEksempel = "{" +
            "  \"avsender\": {\n" +
            "    \"adresselinje1\": \"Første adresselinje\",\n" +
            "    \"adresselinje2\": \"Andre adresselinje\",\n" +
            "    \"adresselinje3\": \"Tredje adresselinje\",\n" +
            "    \"navn\": \"Tester testmann\",\n" +
            "    \"poststed\": \"Teststad\",\n" +
            "    \"postnr\": \"3333\"\n" +
            "  },\n" +
            "  \"mottaker\": {\n" +
            "    \"adresse1\": \"Første adresselinje\",\n" +
            "    \"adresse2\": \"Andre adresselinje\",\n" +
            "    \"adresse3\": null,\n" +
            "    \"postnr\": \"5258\",\n" +
            "    \"poststed\": \"Blomsterdalen\",\n" +
            "    \"navn\": \"Orgnavn\",\n" +
            "    \"land\": \"Norge\",\n" +
            "    \"orgnr\": \"999888777\"," +
            "    \"fnr\": \"22334455566\"\n" +
            "  },\n" +
            "  \"id\": \"AAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
            "  \"tittel\": \"En tittel\",\n" +
            "  \"date\": 1412668736853, \n" +
            "  \"metadataFraAvleverendeSystem\": {\n" +
            "    \"sakssekvensnummer\": 0,\n" +
            "    \"saksaar\": 0,\n" +
            "    \"journalaar\": 0,\n" +
            "    \"journalsekvensnummer\": 0,\n" +
            "    \"journalpostnummer\": 0,\n" +
            "    \"journalposttype\": \"U\",\n" +
            "    \"journalstatus\": null,\n" +
            "    \"journaldato\": null,\n" +
            "    \"dokumentetsDato\": null,\n" +
            "    \"tittel\": null,\n" +
            "    \"saksBehandler\": null,\n" +
            "    \"ekstraMetadata\": [\n" +
            "      {\n" +
            "        \"key\": null,\n" +
            "        \"value\": null\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"metadataForImport\": {\n" +
            "    \"sakssekvensnummer\": 0,\n" +
            "    \"saksaar\": 0,\n" +
            "    \"journalposttype\": null,\n" +
            "    \"journalstatus\": \"I\",\n" +
            "    \"dokumentetsDato\": \"2014-10-21T09:30:13.310+02:00\",\n" +
            "    \"tittel\": null\n" +
            "  },\n" +
            "  \"status\": \"Akseptert\",\n" +
            "  \"niva\": \"3\",\n" +
            "  \"filmetadata\": [\n" +
            "    {\n" +
            "      \"filnavn\": \"dokument-d1c6d795.pdf\",\n" +
            "      \"mimetype\": \"application/pdf\",\n" +
            "      \"sha256hash\": \"caaa6a09e4b5ad571c596dd31fb93689d402834a1b92ff660abeb59c534c088e\",\n" +
            "      \"dokumentType\": null,\n" +
            "      \"size\": 234563 " +
            "    }\n" +
            "  ],\n" +
            "  \"svarSendesTil\": {\n" +
            "    \"adresse1\": \"Første adresselinje\",\n" +
            "    \"adresse2\": \"Andre adresselinje\",\n" +
            "    \"adresse3\": null,\n" +
            "    \"postnr\": \"5258\",\n" +
            "    \"poststed\": \"Blomsterdalen\",\n" +
            "    \"navn\": \"Orgnavn\",\n" +
            "    \"land\": \"Norge\",\n" +
            "    \"orgnr\": \"999888777\",\n" +
            "    \"fnr\": \"22334455566\"\n" +
            "  },\n" +
            "  \"svarPaForsendelse\": \"BBBBBB-BBBB-CCCC-BBBB-BBBBBBBBBB\",\n" +
            "  \"forsendelseType\": \"forsendelseType sett av avsender(heter dokumentType i v5 av servicen)\",\n" +
            "  \"eksternRef\": \"en ref fra avsender\",\n" +
            "  \"downloadUrl\": \"https://svarut.ks.no/tjenester/svarinn/forsendelse/AAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\"\n" +
            "}";

    SvarInnNextMoveConverter converter;
    ObjectMapper objectMapper;
    Validator validator;

    public SvarInnNextMoveConverterTest() throws SAXException {
        converter = new SvarInnNextMoveConverter(
                null,
                null,
                null,
                null,
                null,
                new ArkivmeldingUtil(),
                new UUIDGenerator());

        objectMapper = new ObjectMapper();

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(getFile("../nextmove/src/main/resources/xsd/arkivmelding.xsd"));
        Schema schema = factory.newSchema(schemaFile);
        validator = schema.newValidator();
    }

    @Test
    public void test() throws IOException, SAXException {
        Forsendelse forsendelse = objectMapper.readValue(ksEksempel, Forsendelse.class);
        ByteArrayResource arkivmelding = converter.getArkivmelding(forsendelse);
        String s = new String(arkivmelding.getByteArray());
        System.out.println(s);
        validator.validate(new ByteArraySource(arkivmelding.getByteArray()));
    }

    @Test
    public void testXsDateBefore1970BugFix() throws JAXBException {
        ObjectFactory of = new ObjectFactory();
        Journalpost journalpost = of.createJournalpost();
        String ok =  "-62135596800000";
        String nok = "-62135769799999";
        journalpost.setJournaldato(DateTimeUtil.toXMLGregorianCalendar(Long.parseLong(nok)));
        JAXBContext c = JAXBContext.newInstance(Arkivmelding.class);

        Arkivmelding am = of.createArkivmelding();
        am.getRegistrering().add(journalpost);
        StringWriter stringWriter = new StringWriter();
        c.createMarshaller().marshal(am, stringWriter);

        String s = stringWriter.toString();
        String starttag = "<journaldato>";
        String endtag = "</journaldato>";
        String journaldato = s.substring(s.indexOf(starttag) + starttag.length(), s.indexOf(endtag));

        assertEquals("0001-01-01+01:00", journaldato);
    }

}
