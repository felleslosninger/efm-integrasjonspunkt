package no.difi.meldingsutveksling.oxalisexchange;

/**
 * Created by kubkaray on 28.11.2014.
 */
public class Kvittering {
    private String type;
    private long timeStamp;

    public Kvittering(String type) {
        this.type = type;
        this.timeStamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
