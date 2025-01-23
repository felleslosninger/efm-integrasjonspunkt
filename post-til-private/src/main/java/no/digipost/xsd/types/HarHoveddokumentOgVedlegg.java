package no.digipost.xsd.types;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public interface HarHoveddokumentOgVedlegg<D> {

    D getHoveddokument();
    List<D> getVedleggs();

    default Stream<D> alleDokumenter() {
        return concat(Stream.of(getHoveddokument()), getVedleggs().stream()).sequential();
    }

}
