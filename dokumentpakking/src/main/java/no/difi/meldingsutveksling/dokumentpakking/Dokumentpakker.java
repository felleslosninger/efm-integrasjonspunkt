package no.difi.meldingsutveksling.dokumentpakking;

import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Dokumentpakker {

    private final SignatureHelper signatureHelper;
    private CmsUtil encryptPayload;
    private CreateSBD createSBD;
    private CreateAsice createAsice;

    public Dokumentpakker(SignatureHelper signatureHelper) {
        createSBD = new CreateSBD();
        encryptPayload = new CmsUtil();
        createAsice = new CreateAsice();
        this.signatureHelper = signatureHelper;
    }

    public byte[] pakkTilByteArray(ByteArrayFile document, Avsender avsender, Mottaker mottaker, String id, String type) throws IOException {
        StandardBusinessDocument doc = pakk(document, avsender, mottaker, id, type);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MarshalSBD.marshal(doc, os);
        return os.toByteArray();
    }

    public StandardBusinessDocument pakk(ByteArrayFile document, Avsender avsender, Mottaker mottaker, String id, String type) throws IOException {
        byte[] bytes = createAsice.createAsice(document, signatureHelper, avsender, mottaker).getBytes();
        Payload payload = new Payload(encryptPayload.createCMS(bytes
                , mottaker.getSertifikat()));
        return createSBD.createSBD(avsender.getOrgNummer(), mottaker.getOrgNummer(), payload, id, type);
    }


}
