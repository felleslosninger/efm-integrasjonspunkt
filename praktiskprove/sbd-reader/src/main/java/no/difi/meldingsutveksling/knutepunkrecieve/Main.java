package no.difi.meldingsutveksling.knutepunkrecieve;

import no.difi.meldingsutveksling.knutepunkrecieve.exception.CouldNotLoadXML;
import no.difi.meldingsutveksling.knutepunkrecieve.exception.NoValidProcessId;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.apache.commons.cli.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    Receive receive;
    CommandLine cmd;
    Options options;
    Path userDir = Paths.get(System.getProperty("user.dir")).normalize();

    public Main(String[] args) throws ParseException, IOException {
        options = new CliOptions();
        cmd = new PosixParser().parse(options, args);
    }

    public static void main(String[] args) throws IOException, JAXBException, ParseException {
        new Main(args).run();
    }

    private void run() throws IOException, JAXBException {
        initializeReceiver();
        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sbdreader", options);
        } else if (cmd.hasOption("d")) {
            File workingDir = findWorkingDir();
            sendAllSbdsInDir(workingDir);
        } else if (cmd.hasOption("f")) {
            String uri = cmd.getOptionValue("f");
            StreamSource stream;
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                stream = new StreamSource(new URL(uri).openStream());
            } else {
                stream = new StreamSource(Paths.get(uri).normalize().toFile());
            }
            try {
                handleStream(stream);
            } catch (CouldNotLoadXML | NoValidProcessId e) {
                throw new IllegalArgumentException(e);
            }

        } else {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sbdreader", options);
        }
    }

    private File findWorkingDir() {
        File workingDir;
        if (cmd.getOptionValue("d") == null || cmd.getOptionValue("d").equals(".")) {
            workingDir = userDir.toFile();
        } else {
            workingDir = Paths.get(cmd.getOptionValue("d")).normalize().toFile();
        }
        if (!workingDir.isDirectory())
            throw new IllegalArgumentException("provided input folder is not a folder");
        return workingDir;
    }

    private void sendAllSbdsInDir(File workingDir) {
        for (File f : workingDir.listFiles()) {
            if (f.isDirectory() && cmd.hasOption("r"))
                sendAllSbdsInDir(f);
            else if (f.getAbsolutePath().endsWith(".xml")) {
                StreamSource stream = new StreamSource(f);
                try {
                    handleStream(stream);
                    System.out.println("Delivered: " + findReletivePathFromUserDirectory(f));

                } catch (CouldNotLoadXML e) {
                    System.out.println("Could not read " + findReletivePathFromUserDirectory(f) + " as a standard Business Document.");
                } catch (NoValidProcessId e) {
                    System.out.println(findReletivePathFromUserDirectory(f) + " does not have any acceptable PROCESSIDs");
                } catch (SOAPFaultException e) {
                    System.out.println("Failed sending " + findReletivePathFromUserDirectory(f) + ": " + e.getMessage());
                }
            }
        }
    }

    private String findReletivePathFromUserDirectory(File file) {
        return userDir.relativize(file.toPath()).toString();
    }

    private void handleStream(StreamSource stream) throws CouldNotLoadXML, NoValidProcessId {
        StandardBusinessDocument sbd;
        try {
            sbd = unMarshalSBD(stream);
            if (sbd == null || sbd.getStandardBusinessDocumentHeader() == null) {
                throw new CouldNotLoadXML();
            }
            receive.callReceive(sbd);
        } catch (JAXBException e) {
            throw new CouldNotLoadXML(e);
        }
    }

    private StandardBusinessDocument unMarshalSBD(StreamSource stream) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContext.newInstance(StandardBusinessDocument.class).createUnmarshaller();
        StandardBusinessDocument sbd = unmarshaller.unmarshal(stream, StandardBusinessDocument.class).getValue();
        return sbd;
    }

    private void initializeReceiver() throws IOException {

        if (!cmd.hasOption("k"))
            receive = new Receive("http://localhost:9090/integrasjonspunkt/receive");
        else
            receive = new Receive(cmd.getOptionValue("k"));

    }
}
