package no.difi.meldingsutveksling.knutepunkrecieve;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import no.difi.meldingsutveksling.knutepunkrecieve.exception.CouldNotLoadXML;
import no.difi.meldingsutveksling.knutepunkrecieve.exception.NoValidProcessId;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.Scope;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class Main {

	Receive receive;
	CommandLine cmd;
	Options options;

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
			File workingDir = Paths.get(cmd.getOptionValue("d")).normalize().toFile().getCanonicalFile();
			if (!workingDir.isDirectory())
				throw new IllegalArgumentException("provided input folder is not a folder");
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

	private void sendAllSbdsInDir(File workingDir) {
		for (File f : workingDir.listFiles()) {
			if(f.isDirectory() && cmd.hasOption("r"))
				sendAllSbdsInDir(f);
			else if (f.getAbsolutePath().endsWith(".xml")) {
				StreamSource stream = new StreamSource(f);
				try {
					handleStream(stream);
					System.out.println("Delivered: " + f.getName());
					
				} catch (CouldNotLoadXML e) {
					System.out.println("Could not read " + f.getName() + " as a standard Business Document.");
				} catch (NoValidProcessId e) {

					System.out.println(f.getName() + " does not have any acceptable PROCESSIDs");
				}
			}
		}
	}

	private void handleStream(StreamSource stream) throws CouldNotLoadXML, NoValidProcessId {
		StandardBusinessDocument sbd;
		try {
			sbd = unMarshalSBD(stream);
			if (sbd == null || sbd.getStandardBusinessDocumentHeader() == null) throw new CouldNotLoadXML();
			for (Scope scope : sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope()) {
				if (scope.getType().equals("PROCESSID") && scope.getInstanceIdentifier().equals("urn:www.difi.no:profile:meldingsutveksling:ver1.0")) {
					//receive.callReceive(sbd);
					return;
				}
			}
			throw new NoValidProcessId();
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
			receive = new Receive("http://localhost:9090/knutepunkt/receive");
		else
			receive = new Receive(cmd.getOptionValue("k"));

	}
}
