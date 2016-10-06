/**
 *
 * Modified version of samples/org/apache/xml/security/samples/signature/VerifySignature.java
 * of how to validate XML signatures from the Santuario project http://santuario.apache.org/
 *
 * What is modified:
 * removed some unnecessary assignments of null.
 * Removed try/catch with System.out
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package no.difi.meldingsutveksling.kvittering;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.samples.DSNamespaceContext;
import org.apache.xml.security.samples.utils.resolver.OfflineResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
class DocumentValidator {

    public static boolean validate(Document doc) throws XMLSecurityException, XPathExpressionException {
        org.apache.xml.security.Init.init();
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(new DSNamespaceContext());

        String expression = "//ds:Signature[1]";
        Element sigElement = (Element) xpath.evaluate(expression, doc, XPathConstants.NODE);
        org.apache.xml.security.signature.XMLSignature signature = new org.apache.xml.security.signature.XMLSignature(sigElement, "");

        signature.addResourceResolver(new OfflineResolver());

        KeyInfo ki = signature.getKeyInfo();

        boolean isValid = false;
        if (ki != null) {
            X509Certificate cert = signature.getKeyInfo().getX509Certificate();

            if (cert != null) {
                isValid = signature.checkSignatureValue(cert);
            } else {
                PublicKey pk;
                pk = signature.getKeyInfo().getPublicKey();

                if (pk != null) {
                    isValid = signature.checkSignatureValue(pk);
                }
            }
        }

        return isValid;
    }
}