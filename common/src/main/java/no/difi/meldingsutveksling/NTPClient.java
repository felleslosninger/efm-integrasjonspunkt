package no.difi.meldingsutveksling;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class NTPClient {

    private NTPUDPClient client;
    private InetAddress host;

    public NTPClient(String host) throws UnknownHostException {
        this.host = InetAddress.getByName(host);
        client = new NTPUDPClient();
    }

    public long getOffset() throws IOException {
        TimeInfo info = client.getTime(host);
        info.computeDetails();
        return info.getOffset();
    }

}
