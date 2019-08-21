/**
 * From https://svn.apache.org/.../trunk/samples/org/apache/xml/security/samples/DSNamespaceContext.java
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.samples;

import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A NamespaceContext implementation for digital signatures
 */
public class DSNamespaceContext implements NamespaceContext {

    private Map<String, String> namespaceMap = new HashMap<>();

    public DSNamespaceContext() {
        namespaceMap.put("ds", "http://www.w3.org/2000/09/xmldsig#");
    }

    public DSNamespaceContext(Map<String, String> namespaces) {
        this();
        namespaceMap.putAll(namespaces);
    }

    public String getNamespaceURI(String arg0) {
        return namespaceMap.get(arg0);
    }

    public void putPrefix(String prefix, String namespace) {
        namespaceMap.put(prefix, namespace);
    }

    public String getPrefix(String arg0) {
        return namespaceMap.entrySet().stream()
                .filter(p -> p.getValue().equals(arg0))
                .findAny()
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public Iterator<String> getPrefixes(String arg0) {
        return namespaceMap.keySet().iterator();
    }
}