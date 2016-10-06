/**
 *
 * Modified version of xml-security-java/trunk/samples/org/apache/xml/security/samples/utils/resolver/OfflineResolver.java
 * from http://santuario.apache.org/
 * Made changes to be compatible with our currently used version of xmlsec.
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
package org.apache.xml.security.samples.utils.resolver;

import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.resolver.ResourceResolverContext;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class helps us home users to resolve http URIs without a network
 * connection
 *
 * @author $Author$
 */
public class OfflineResolver extends ResourceResolverSpi {

    static org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(OfflineResolver.class);

    /** Field _uriMap */
    static Map<String, String> _uriMap = null;

    /** Field _mimeMap */
    static Map<String, String> _mimeMap = null;

    static {
        org.apache.xml.security.Init.init();

        OfflineResolver._uriMap = new HashMap<String, String>();
        OfflineResolver._mimeMap = new HashMap<String, String>();

        OfflineResolver.register("http://www.w3.org/TR/xml-stylesheet",
                "samples/data/org/w3c/www/TR/xml-stylesheet.html",
                "text/html");
        OfflineResolver.register("http://www.w3.org/TR/2000/REC-xml-20001006",
                "samples/data/org/w3c/www/TR/2000/REC-xml-20001006",
                "text/xml");
        OfflineResolver.register("http://www.nue.et-inf.uni-siegen.de/index.html",
                "samples/data/org/apache/xml/security/temp/nuehomepage",
                "text/html");
        OfflineResolver.register("http://www.nue.et-inf.uni-siegen.de/~geuer-pollmann/id2.xml",
                "samples/data/org/apache/xml/security/temp/id2.xml", "text/xml");
        OfflineResolver.register("http://xmldsig.pothole.com/xml-stylesheet.txt",
                "samples/data/com/pothole/xmldsig/xml-stylesheet.txt", "text/xml");
        OfflineResolver.register("http://www.w3.org/Signature/2002/04/xml-stylesheet.b64",
                "samples/data/ie/baltimore/merlin-examples/merlin-xmldsig-twenty-three/xml-stylesheet.b64", "text/plain");
    }

    /**
     * Method register
     *
     * @param URI
     * @param filename
     * @param MIME
     */
    private static void register(String URI, String filename, String MIME) {
        OfflineResolver._uriMap.put(URI, filename);
        OfflineResolver._mimeMap.put(URI, MIME);
    }

    private static URI getNewURI(String uri, String baseURI) throws URISyntaxException {
        URI newUri;
        if (baseURI == null || "".equals(baseURI)) {
            newUri = new URI(uri);
        } else {
            newUri = new URI(baseURI).resolve(uri);
        }

        // if the URI contains a fragment, ignore it
        if (newUri.getFragment() != null) {
            return new URI(newUri.getScheme(), newUri.getSchemeSpecificPart(), null);
        }
        return newUri;
    }

    @Override
    public XMLSignatureInput engineResolveURI(ResourceResolverContext context) throws ResourceResolverException {
        final String uriToResolve = context.uriToResolve;
        try {

            if (OfflineResolver._uriMap.containsKey(uriToResolve)) {
                String newURI = OfflineResolver._uriMap.get(uriToResolve);

                log.debug("Mapped " + uriToResolve + " to " + newURI);

                InputStream is = new FileInputStream(newURI);

                log.debug("Available bytes = " + is.available());

                XMLSignatureInput result = new XMLSignatureInput(is);

                // XMLSignatureInput result = new XMLSignatureInput(inputStream);
                result.setSourceURI(uriToResolve);
                result.setMIMEType(OfflineResolver._mimeMap.get(uriToResolve));

                return result;
            } else {
                Object exArgs[] = {"The URI " + uriToResolve + " is not configured for offline work"};

                throw new ResourceResolverException("generic.EmptyMessage", exArgs, uriToResolve, context.baseUri);
            }
        } catch (IOException ex) {
            throw new ResourceResolverException(ex, uriToResolve, context.baseUri, "generic.EmptyMessage");
        }
    }

    @Override
    public boolean engineCanResolveURI(ResourceResolverContext context) {
        String uriNodeValue = context.uriToResolve;
        if (uriNodeValue.equals("") || uriNodeValue.startsWith("#")) {
            return false;
        }

        try {
            URI uriNew = getNewURI(uriNodeValue, context.baseUri);
            if (uriNew.getScheme().equals("http")) {
                log.debug("I state that I can resolve " + uriNew.toString());
                return true;
            }

            log.debug("I state that I can't resolve " + uriNew.toString());
        } catch (URISyntaxException ex) {
            //
        }

        return false;
    }
}