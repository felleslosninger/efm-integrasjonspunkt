package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSAESOAEPparams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OutputEncryptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

public class CmsUtil {

    private final ASN1ObjectIdentifier cmsEncryptionAlgorithm;
    private final AlgorithmIdentifier keyEncryptionScheme;

    public CmsUtil() {
        Security.addProvider(new BouncyCastleProvider());

        keyEncryptionScheme = rsaesOaepIdentifier();
        cmsEncryptionAlgorithm = CMSAlgorithm.AES256_CBC;
    }

    private AlgorithmIdentifier rsaesOaepIdentifier() {
        AlgorithmIdentifier hash = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256, DERNull.INSTANCE);
        AlgorithmIdentifier mask = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1, hash);
        AlgorithmIdentifier p_source = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_pSpecified, new DEROctetString(new byte[0]));
        ASN1Encodable parameters = new RSAESOAEPparams(hash, mask, p_source);
        return new AlgorithmIdentifier(PKCSObjectIdentifiers.id_RSAES_OAEP, parameters);
    }

    public byte[] createCMS(byte[] bytes, X509Certificate sertifikat) {
        try {
            JceKeyTransRecipientInfoGenerator recipientInfoGenerator = new JceKeyTransRecipientInfoGenerator(sertifikat, keyEncryptionScheme);

            CMSEnvelopedDataGenerator envelopedDataGenerator = new CMSEnvelopedDataGenerator();
            envelopedDataGenerator.addRecipientInfoGenerator(recipientInfoGenerator);

            OutputEncryptor contentEncryptor = new JceCMSContentEncryptorBuilder(cmsEncryptionAlgorithm).build();
            CMSEnvelopedData cmsData = envelopedDataGenerator.generate(new CMSProcessableByteArray(bytes), contentEncryptor);

            return cmsData.getEncoded();

        } catch (CertificateEncodingException e) {
            throw new MeldingsUtvekslingRuntimeException("Feil med mottakers sertifikat", e);
        } catch (CMSException e) {
            throw new MeldingsUtvekslingRuntimeException("Kunne ikke generere Cryptographic Message Syntax for dokumentpakke", e);
        } catch (IOException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public byte[] decryptCMS(byte[] encrypted, PrivateKey privateKey) {
        try {
            CMSEnvelopedData cms;

            cms = new CMSEnvelopedData(new ByteArrayInputStream(encrypted));
            RecipientInformationStore recipients = cms.getRecipientInfos();
            Collection<?> c = recipients.getRecipients();
            Iterator<?> it = c.iterator();
            byte[] unEncryptedData = null;
            if (it.hasNext()) {
                RecipientInformation recipient = (RecipientInformation) it.next();
                unEncryptedData = recipient.getContent(new JceKeyTransEnvelopedRecipient(privateKey));
            }
            if (it.hasNext()) {
                throw new IllegalArgumentException("CMS-package has more than one recipient. Only one expected");
            }

            return unEncryptedData;
        } catch (CMSException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

}
