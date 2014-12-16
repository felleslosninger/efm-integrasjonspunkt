package no.difi.meldingsutveksling.knutepunkrecieve;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Test;


public class MainTest {

	
	@Test
	public void test () throws IOException{
		System.out.println(Paths.get(System.getProperty("user.dir")).normalize().toFile().isDirectory());
		System.out.println(Paths.get(System.getProperty("user.dir")).toString());
	}
}
