package no.difi.meldingsutveksling.ptv.mapping;

public class Attachment {

    private String filename;
    private String name;
    private byte[] data;

    public Attachment(String filename, String name, byte[] data) {
        this.filename = filename;
        this.name = name;
        this.data = data;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Attachment{" +
                "filename='" + filename + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
