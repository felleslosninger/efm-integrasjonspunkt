package no.difi.meldingsutveksling.knutepunkrecieve;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;

public class Main {

	File workingDir; 
	Receive receive;
	
	
	public Main(String[] args) throws ParseException, IOException {
		Options options = new Options();
		
		OptionBuilder.withArgName( "inFolder" )
                .hasArg()
                .withDescription(  "inFolder" )
                .create( "inFolder");
		options.addOption(OptionBuilder.create("inFolder"));
		
		OptionBuilder.withArgName( "knutepunkt" )
                .hasArg()
                .withDescription(  "knutepunkt" );
        options.addOption(OptionBuilder .create( "knutepunkt"));
		
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse( options, args);
		
		if (!cmd.hasOption("inFolder")) workingDir = new File("c:\\data\\oxalis-filer").getCanonicalFile();
		else workingDir = new File(cmd.getOptionValue("inFolder"));
		if (!workingDir.isDirectory()) throw new IllegalArgumentException("provided input folder is not a folder");
		if (!cmd.hasOption("knutepunkt")) receive = new Receive("http://localhost:9090/knutepunkt/receive");
		else receive = new Receive(cmd.getOptionValue("knutepunkt"));
		
		
		
	}

	public static void main(String[] args) throws IOException, JAXBException, ParseException {
		new Main(args).run();
	}

	private void run() throws IOException, JAXBException {
		for(File f : workingDir.listFiles()){
			if(f.getAbsolutePath().endsWith(".xml")){
				Unmarshaller unmarshaller = JAXBContext.newInstance(StandardBusinessDocument.class).createUnmarshaller();
				StandardBusinessDocument sbd  = unmarshaller.unmarshal(new StreamSource(f), StandardBusinessDocument.class).getValue();
				System.out.println(sbd.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue());

				receive.sendEduMeldig(sbd);
			}
		}
	}
}
