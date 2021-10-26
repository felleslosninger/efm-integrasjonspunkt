package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Data;

import java.util.List;

@Data
public class Parcel {

    no.difi.meldingsutveksling.dpi.client.domain.Document mainDocument;
    List<no.difi.meldingsutveksling.dpi.client.domain.Document> attachments;
}