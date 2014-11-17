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
package no.difi.meldingsutveksling.dokumentpakking.crypto;

import java.util.Arrays;

import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;


public class Signature implements AsicEAttachable {

    private final byte[] xmlBytes;

    public Signature(byte[] xmlBytes) {
        this.xmlBytes = Arrays.copyOf(xmlBytes, xmlBytes.length);
    }

    @Override
    public String getFileName() {
        return "META-INF/signatures.xml";
    }

    public byte[] getBytes() {
        return Arrays.copyOf(xmlBytes, xmlBytes.length);
    }

    @Override
    public String getMimeType() {
        return "application/xml";
    }
}
