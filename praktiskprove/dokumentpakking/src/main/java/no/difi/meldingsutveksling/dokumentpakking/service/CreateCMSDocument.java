/**
 * Copyright (C) Posten Norge AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.dokumentpakking.crypto.Sertifikat;
import no.difi.meldingsutveksling.dokumentpakking.domain.CMSDocument;

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
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OutputEncryptor;

import java.io.IOException;
import java.security.Security;
import java.security.cert.CertificateEncodingException;

public class CreateCMSDocument {

    private final ASN1ObjectIdentifier cmsEncryptionAlgorithm;
    private final AlgorithmIdentifier keyEncryptionScheme;

    public CreateCMSDocument() {
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

    public CMSDocument createCMS(byte[] bytes, Sertifikat sertifikat) {
        try {
            JceKeyTransRecipientInfoGenerator recipientInfoGenerator = new JceKeyTransRecipientInfoGenerator(sertifikat.getX509Certificate(), keyEncryptionScheme)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);

            CMSEnvelopedDataGenerator envelopedDataGenerator = new CMSEnvelopedDataGenerator();
            envelopedDataGenerator.addRecipientInfoGenerator(recipientInfoGenerator);

            OutputEncryptor contentEncryptor = new JceCMSContentEncryptorBuilder(cmsEncryptionAlgorithm).build();
            CMSEnvelopedData cmsData = envelopedDataGenerator.generate(new CMSProcessableByteArray(bytes),
                    contentEncryptor);

            return new CMSDocument(cmsData.getEncoded());

        } catch (CertificateEncodingException e) {
            throw new RuntimeException("Feil med mottakers sertifikat", e);
        } catch (CMSException e) {
            throw new RuntimeException("Kunne ikke generere Cryptographic Message Syntax for dokumentpakke", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
