package no.difi.virksert.server.model;

import no.difi.certvalidator.extra.NorwegianOrganizationNumberRule;
import no.difi.xsd.virksert.model._1.Certificate;

import javax.persistence.*;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

import static no.difi.certvalidator.extra.NorwegianOrganizationNumberRule.*;

@Entity
@Table(name = "tbl_Registration", indexes = {
        @Index(name = "identifier", columnList = "identifier"),
        @Index(name = "expiration", columnList = "expiration"),
        @Index(name = "revoked", columnList = "revoked"),
        @Index(name = "updated", columnList = "updated")
})
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String identifier;
    @Lob
    private byte[] certificate;
    private String serialNumber;
    private long expiration;
    private long revoked;
    private long updated;

    public Registration() {
    }

    public void update(X509Certificate certificate) {
        try {
            final NorwegianOrganization norwegianOrganization = extractNumber(certificate);
            if (norwegianOrganization == null) {
                throw new IllegalStateException("no Norwegian organisation number in X509 certificate");
            }
            setIdentifier(norwegianOrganization.getNumber());
            if (!Arrays.equals(certificate.getEncoded(), getCertificate())) {
                setCertificate(certificate.getEncoded());
                setSerialNumber(String.valueOf(certificate.getSerialNumber()));
                setExpiration(certificate.getNotAfter().getTime());
                setUpdated(System.currentTimeMillis());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public void setCertificate(byte[] certificate) {
        this.certificate = certificate;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getRevoked() {
        return revoked;
    }

    public void setRevoked(long revoked) {
        this.revoked = revoked;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public Certificate toCertificate() {
        Certificate certificate = new Certificate();
        certificate.setIdentifier(getIdentifier());
        certificate.setSerialnumber(getSerialNumber());
        certificate.setExpiration(getExpiration());
        certificate.setUpdated(getUpdated());

        return certificate;
    }

    private XMLGregorianCalendar convertToXmlGregorianCalendar(long timestamp) {
        try {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(new Date(timestamp));
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "Registration{" +
                "identifier='" + identifier + '\'' +
                ", expiration=" + expiration +
                '}';
    }
}
