package no.difi.meldingsutveksling.ptv;

public class ClientTest {
    public static void main(String[] args) {
        //ApplicationContext ctx = new FileSystemXmlApplicationContext("src/test/resources/applicationContext.xml");
        Client client = new Client();

        client.insertCorrespondence();
    }
}