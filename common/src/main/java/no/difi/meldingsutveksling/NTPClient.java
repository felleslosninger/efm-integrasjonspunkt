package no.difi.meldingsutveksling;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class NTPClient {

    private NTPUDPClient ntpClient;
    private InetAddress host;

    public NTPClient(String host) throws UnknownHostException {
        this.host = InetAddress.getByName(host);
        ntpClient = new NTPUDPClient();
    }

    public long getOffset() throws IOException {
        TimeInfo info = ntpClient.getTime(host);
        info.computeDetails();
        return info.getOffset();
    }

}
