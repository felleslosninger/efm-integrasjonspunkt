package no.difi.meldingsutveksling.nextmove.v2;

import lombok.Builder;
import lombok.Value;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

@Value
@Builder
public final class ContentDisposition {

    private static final String CHARSET_SHOULD_BE_UTF_8_OR_ISO_8859_1 = "Charset should be UTF-8 or ISO-8859-1";

    private final String type;
    private final String name;
    private final String filename;
    private final Charset filenameCharset;
    private final Charset nameCharset;
    private final Long size;
    private final OffsetDateTime creationDate;
    private final OffsetDateTime modificationDate;
    private final OffsetDateTime readDate;


    /**
     * Return the header value for this content disposition as defined in RFC 2183.
     *
     * @see #parse(String)
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.type != null) {
            sb.append(this.type);
        }
        if (this.name != null) {
            if (this.nameCharset == null || StandardCharsets.US_ASCII.equals(this.nameCharset)) {
                sb.append("; name=\"");
                sb.append(this.name).append('\"');
            } else {
                sb.append("; name*=");
                sb.append(encodeHeaderFieldParam(this.name, this.nameCharset));
            }
        }
        if (this.filename != null) {
            if (this.filenameCharset == null || StandardCharsets.US_ASCII.equals(this.filenameCharset)) {
                sb.append("; filename=\"");
                sb.append(this.filename).append('\"');
            } else {
                sb.append("; filename*=");
                sb.append(encodeHeaderFieldParam(this.filename, this.filenameCharset));
            }
        }
        if (this.size != null) {
            sb.append("; size=");
            sb.append(this.size);
        }
        if (this.creationDate != null) {
            sb.append("; creation-date=\"");
            sb.append(RFC_1123_DATE_TIME.format(this.creationDate));
            sb.append('\"');
        }
        if (this.modificationDate != null) {
            sb.append("; modification-date=\"");
            sb.append(RFC_1123_DATE_TIME.format(this.modificationDate));
            sb.append('\"');
        }
        if (this.readDate != null) {
            sb.append("; read-date=\"");
            sb.append(RFC_1123_DATE_TIME.format(this.readDate));
            sb.append('\"');
        }
        return sb.toString();
    }

    /**
     * Return an empty content disposition.
     *
     * @return {@link ContentDisposition}
     */
    public static ContentDisposition empty() {
        return new ContentDisposition("", null, null, null, null, null, null, null, null);
    }

    /**
     * Parse a {@literal Content-Disposition} header value as defined in RFC 2183.
     *
     * @param contentDisposition the {@literal Content-Disposition} header value
     * @return the parsed content disposition
     * @see #toString()
     */
    @SuppressWarnings("squid:S3776")
    public static ContentDisposition parse(String contentDisposition) {
        List<String> parts = tokenize(contentDisposition);

        ContentDispositionBuilder builder = ContentDisposition.builder()
                .type(parts.get(0));

        String filename = null;
        String name = null;
        for (int i = 1; i < parts.size(); i++) {
            String part = parts.get(i);
            int eqIndex = part.indexOf('=');
            if (eqIndex != -1) {
                String attribute = part.substring(0, eqIndex);
                String value = (part.startsWith("\"", eqIndex + 1) && part.endsWith("\"") ?
                        part.substring(eqIndex + 2, part.length() - 1) :
                        part.substring(eqIndex + 1));
                if (attribute.equals("name*")) {
                    name = decodeHeaderFieldParam(value);
                    builder.name(name);
                    Charset charset = Charset.forName(value.substring(0, value.indexOf('\'')));
                    builder.nameCharset(charset);
                    Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
                            CHARSET_SHOULD_BE_UTF_8_OR_ISO_8859_1);
                } else if (attribute.equals("name") && (name ==  null)) {
                    name = value;
                    builder.name(name);
                } else if (attribute.equals("filename*")) {
                    filename = decodeHeaderFieldParam(value);
                    builder.filename(filename);
                    Charset charset = Charset.forName(value.substring(0, value.indexOf('\'')));
                    builder.filenameCharset(charset);
                    Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
                            CHARSET_SHOULD_BE_UTF_8_OR_ISO_8859_1);
                } else if (attribute.equals("filename") && (filename == null)) {
                    filename = value;
                    builder.filename(filename);
                } else if (attribute.equals("size")) {
                    builder.size(Long.parseLong(value));
                } else if (attribute.equals("creation-date")) {
                    try {
                        builder.creationDate(OffsetDateTime.parse(value, RFC_1123_DATE_TIME));
                    } catch (DateTimeParseException ex) {
                        // ignore
                    }
                } else if (attribute.equals("modification-date")) {
                    try {
                        builder.modificationDate(OffsetDateTime.parse(value, RFC_1123_DATE_TIME));
                    } catch (DateTimeParseException ex) {
                        // ignore
                    }
                } else if (attribute.equals("read-date")) {
                    try {
                        builder.readDate(OffsetDateTime.parse(value, RFC_1123_DATE_TIME));
                    } catch (DateTimeParseException ex) {
                        // ignore
                    }
                }
            } else {
                throw new IllegalArgumentException("Invalid content disposition format");
            }
        }
        return builder.build();
    }

    private static List<String> tokenize(String headerValue) {
        int index = headerValue.indexOf(';');
        String type = (index >= 0 ? headerValue.substring(0, index) : headerValue).trim();
        if (type.isEmpty()) {
            throw new IllegalArgumentException("Content-Disposition header must not be empty");
        }
        List<String> parts = new ArrayList<>();
        parts.add(type);
        if (index >= 0) {
            do {
                int nextIndex = getNextIndex(headerValue, index);
                String part = headerValue.substring(index + 1, nextIndex).trim();
                if (!part.isEmpty()) {
                    parts.add(part);
                }
                index = nextIndex;
            }
            while (index < headerValue.length());
        }
        return parts;
    }

    private static int getNextIndex(String headerValue, int index) {
        int nextIndex = index + 1;
        boolean quoted = false;
        while (nextIndex < headerValue.length()) {
            char ch = headerValue.charAt(nextIndex);
            if (ch == ';') {
                if (!quoted) {
                    break;
                }
            } else if (ch == '"') {
                quoted = !quoted;
            }
            nextIndex++;
        }
        return nextIndex;
    }

    /**
     * Decode the given header field param as describe in RFC 5987.
     * <p>Only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported.
     *
     * @param input the header field param
     * @return the encoded header field param
     * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
     */
    private static String decodeHeaderFieldParam(String input) {
        Assert.notNull(input, "Input String should not be null");
        int firstQuoteIndex = input.indexOf('\'');
        int secondQuoteIndex = input.indexOf('\'', firstQuoteIndex + 1);
        // US_ASCII
        if (firstQuoteIndex == -1 || secondQuoteIndex == -1) {
            return input;
        }
        Charset charset = Charset.forName(input.substring(0, firstQuoteIndex));
        Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
                CHARSET_SHOULD_BE_UTF_8_OR_ISO_8859_1);
        byte[] value = input.substring(secondQuoteIndex + 1).getBytes(charset);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int index = 0;
        while (index < value.length) {
            byte b = value[index];
            if (isRFC5987AttrChar(b)) {
                bos.write((char) b);
                index++;
            } else if (b == '%') {
                char[] array = {(char) value[index + 1], (char) value[index + 2]};
                bos.write(Integer.parseInt(String.valueOf(array), 16));
                index += 3;
            } else {
                throw new IllegalArgumentException("Invalid header field parameter format (as defined in RFC 5987)");
            }
        }
        return new String(bos.toByteArray(), charset);
    }

    private static boolean isRFC5987AttrChar(byte c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' ||
                c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
    }

    /**
     * Encode the given header field param as describe in RFC 5987.
     *
     * @param input   the header field param
     * @param charset the charset of the header field param string,
     *                only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported
     * @return the encoded header field param
     * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
     */
    private static String encodeHeaderFieldParam(String input, Charset charset) {
        Assert.notNull(input, "Input String should not be null");
        Assert.notNull(charset, "Charset should not be null");
        if (StandardCharsets.US_ASCII.equals(charset)) {
            return input;
        }
        Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
                CHARSET_SHOULD_BE_UTF_8_OR_ISO_8859_1);
        byte[] source = input.getBytes(charset);
        int len = source.length;
        StringBuilder sb = new StringBuilder(len << 1);
        sb.append(charset.name());
        sb.append("''");
        for (byte b : source) {
            if (isRFC5987AttrChar(b)) {
                sb.append((char) b);
            } else {
                sb.append('%');
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                sb.append(hex1);
                sb.append(hex2);
            }
        }
        return sb.toString();
    }
}