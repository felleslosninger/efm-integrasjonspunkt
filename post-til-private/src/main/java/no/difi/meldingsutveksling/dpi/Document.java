package no.difi.meldingsutveksling.dpi;

public class Document {
    private byte[] contents;
    private String mimeType;
    private String fileName;
    private String title;

    public Document(byte[] contents, String mimeType, String fileName, String title) {
        this.contents = contents;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.title = title;
    }

    public byte[] getContents() {
        return contents;
    }
    /**
     *
     * @return MIME type of the document
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     *
     * @return typically the prefix in the filename
     */
    public String getFileName() {
        return fileName;
    }

    /**
     *
     * @return this title will be used to display the post in the target portal
     */

    public String getTitle() {
        return title;
    }
}
