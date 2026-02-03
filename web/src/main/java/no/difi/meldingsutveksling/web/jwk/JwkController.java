package no.difi.meldingsutveksling.web.jwk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

@Controller
public class JwkController {

    private static final List<Integer> ALLOWED_SIZES = List.of(1024, 2048, 4096);

    @GetMapping("/jwk")
    public String indexPage(Model model,
                            @RequestParam(name = "keySize", required = false) Integer keySize,
                            @RequestParam(name = "shortName", required = false) String shortName) {
        model.addAttribute("sizes", ALLOWED_SIZES);
        model.addAttribute("selected", keySize != null && ALLOWED_SIZES.contains(keySize) ? keySize : 2048);
        // short name default
        String name = (shortName == null || shortName.isBlank()) ? "MyOrg" : shortName.trim();
        model.addAttribute("shortName", name);
        // empty defaults so the template can render placeholders
        model.addAttribute("private", "");
        model.addAttribute("public", "");
        return "jwk";
    }

    @PostMapping("/jwk")
    public String createKeys(@RequestParam("keySize") Integer keySize,
                             @RequestParam(name = "shortName", required = false) String shortName,
                             Model model) {
        int size = (keySize != null && ALLOWED_SIZES.contains(keySize)) ? keySize : 2048;
        String name = (shortName == null || shortName.isBlank()) ? "MyOrg" : shortName.trim();
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(size);
            KeyPair kp = kpg.generateKeyPair();

            String publicJwk = toJwkPublic((RSAPublicKey) kp.getPublic());
            String privateJwk = toJwkPrivate((RSAPrivateCrtKey) kp.getPrivate());

            model.addAttribute("sizes", ALLOWED_SIZES);
            model.addAttribute("selected", size);
            model.addAttribute("shortName", name);
            model.addAttribute("private", privateJwk);
            model.addAttribute("public", publicJwk);
            return "jwk";
        } catch (Exception e) {
            model.addAttribute("sizes", ALLOWED_SIZES);
            model.addAttribute("selected", size);
            model.addAttribute("shortName", name);
            model.addAttribute("private", "");
            model.addAttribute("public", "");
            model.addAttribute("error", "Failed to generate RSA key pair: " + e.getMessage());
            return "jwk";
        }
    }

    // --- Helpers: JWK (RFC 7517/7518) for RSA keys ---
    private static String toJwkPublic(RSAPublicKey pub) throws Exception {
        String n = b64u(pub.getModulus());
        String e = b64u(pub.getPublicExponent());
        String kid = jwkThumbprint("RSA", n, e);
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("n", n);
        jwk.put("e", e);
        jwk.put("alg", "RS256");
        jwk.put("kid", kid);
        return toPrettyJson(jwk);
    }

    private static String toJwkPrivate(RSAPrivateCrtKey priv) throws Exception {
        String n = b64u(priv.getModulus());
        String e = b64u(priv.getPublicExponent());
        String kid = jwkThumbprint("RSA", n, e);
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        // include public params too (convenient for some tools)
        jwk.put("n", n);
        jwk.put("e", e);
        jwk.put("alg", "RS256");
        jwk.put("kid", kid);
        // private params per RFC 7518
        jwk.put("d", b64u(priv.getPrivateExponent()));
        jwk.put("p", b64u(priv.getPrimeP()));
        jwk.put("q", b64u(priv.getPrimeQ()));
        jwk.put("dp", b64u(priv.getPrimeExponentP()));
        jwk.put("dq", b64u(priv.getPrimeExponentQ()));
        jwk.put("qi", b64u(priv.getCrtCoefficient()));
        return toPrettyJson(jwk);
    }

    // RFC 7638 JWK Thumbprint over the public members only
    private static String jwkThumbprint(String kty, String n, String e) throws Exception {
        // Lexicographic order of member names: e, kty, n
        String canonical = "{\"e\":\"" + e + "\",\"kty\":\"" + kty + "\",\"n\":\"" + n + "\"}";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static String toPrettyJson(Map<String, Object> map) throws Exception {
        ObjectMapper om = new ObjectMapper();
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(map);
    }

    private static String b64u(BigInteger bi) {
        byte[] bytes = bi.toByteArray();
        // Strip leading zero if present to ensure unsigned big-endian
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
