package no.difi.meldingsutveksling.arkivmelding;

import com.google.common.collect.Lists;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Dokumentbeskrivelse;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.arkivverket.standarder.noark5.arkivmelding.Saksmappe;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class ArkivmeldingUtil {

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
}
