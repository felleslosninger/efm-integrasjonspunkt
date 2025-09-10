package no.difi.meldingsutveksling.shipping.ws;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.ArrayOfFile;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.File;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.ObjectFactory;

public class FileListBuilder {
    String filename;
    String checksum;

    public FileListBuilder withFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public jakarta.xml.bind.JAXBElement<ArrayOfFile> build() {
        no.difi.meldingsutveksling.altinn.mock.brokerbasic.ObjectFactory objectFactory = new ObjectFactory();
        ArrayOfFile arrayOfFile = new ArrayOfFile();
        File file = new File();
        file.setFileName(filename);
        arrayOfFile.getFile().add(file);
        return objectFactory.createArrayOfFile(arrayOfFile);
    }
}
