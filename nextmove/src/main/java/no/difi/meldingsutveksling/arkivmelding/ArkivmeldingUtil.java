package no.difi.meldingsutveksling.arkivmelding;

import com.google.common.collect.Lists;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

public class ArkivmeldingUtil {

    private ArkivmeldingUtil() {
    }

    public static List<String> getFilenames(Arkivmelding am) throws ArkivmeldingException {
        List<String> filenames = Lists.newArrayList();

        getJournalpost(am).getDokumentbeskrivelseAndDokumentobjekt().stream()
                .filter(Dokumentbeskrivelse.class::isInstance)
                .map(Dokumentbeskrivelse.class::cast)
                .flatMap(d -> d.getDokumentobjekt().stream())
                .map(Dokumentobjekt::getReferanseDokumentfil)
                .forEach(filenames::add);

        return filenames;
    }

    public static byte[] marshalArkivmelding(Arkivmelding am) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Arkivmelding.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(am, bos);
        return bos.toByteArray();
    }

    public static Arkivmelding unmarshalArkivmelding(InputStream inputStream) throws JAXBException {
        JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Arkivmelding.class}, new HashMap());
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StreamSource(inputStream), Arkivmelding.class).getValue();
    }

    public static Saksmappe getSaksmappe(Arkivmelding am) {
        return am.getMappe().stream()
                .filter(Saksmappe.class::isInstance)
                .map(Saksmappe.class::cast)
                .findFirst()
                .orElseThrow(() -> new MeldingsUtvekslingRuntimeException("No \"Saksmappe\" found in Arkivmelding"));
    }

    public static Journalpost getJournalpost(Arkivmelding am) {
        return getSaksmappe(am).getBasisregistrering().stream()
                .filter(Journalpost.class::isInstance)
                .map(Journalpost.class::cast)
                .findFirst()
                .orElseThrow(() -> new MeldingsUtvekslingRuntimeException("No \"Journalpost\" found in Arkivmelding"));
    }

    public static XMLGregorianCalendar stringAsXmlGregorianCalendar(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(date, formatter);
        GregorianCalendar gcal = GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault()));
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static XMLGregorianCalendar epochMilliAsXmlGregorianCalendar(String ms) {
        LocalDate localDate = Instant.ofEpochMilli(Long.valueOf(ms)).atZone(ZoneId.systemDefault()).toLocalDate();
        GregorianCalendar gcal = GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault()));
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

}
