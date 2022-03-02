package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Data;

import java.util.List;

@Data
public class Parcel {

    Document mainDocument;
    List<Document> attachments;
}