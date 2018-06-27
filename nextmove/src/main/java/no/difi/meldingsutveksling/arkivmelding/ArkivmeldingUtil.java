package no.difi.meldingsutveksling.arkivmelding;

import com.google.common.collect.Lists;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Dokumentbeskrivelse;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.arkivverket.standarder.noark5.arkivmelding.Saksmappe;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class ArkivmeldingUtil {

    public static final String ARKIVMELDING_XML = "arkivmelding.xml";

    private ArkivmeldingUtil() {
    }

    public static List<String> getFilenames(Arkivmelding am) {
        List<String> filenames = Lists.newArrayList();

        if (am.getMappe().isEmpty() || !(am.getMappe().get(0) instanceof Saksmappe)) {
            return filenames;
        }
        Saksmappe sm = (Saksmappe) am.getMappe().get(0);

        if (sm.getBasisregistrering().isEmpty() || !(sm.getBasisregistrering().get(0) instanceof Journalpost)) {
            return filenames;
        }
        Journalpost jp = (Journalpost) sm.getBasisregistrering().get(0);

        jp.getDokumentbeskrivelseAndDokumentobjekt().stream()
                .filter(d -> d instanceof Dokumentbeskrivelse)
                .map(d -> (Dokumentbeskrivelse)d)
                .flatMap(d -> d.getDokumentobjekt().stream())
                .map(d -> d.getReferanseDokumentfil())
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
}
