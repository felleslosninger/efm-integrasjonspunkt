import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;

public class ApiUtils {

    public static String retrieveAccessToken(String scopes) throws IOException, InterruptedException, JOSEException {
        String pem1 = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGWTCCBEGgAwIBAgILAZl75qx7xrySb8UwDQYJKoZIhvcNAQELBQAwbjELMAkG\n" +
            "A1UEBhMCTk8xGDAWBgNVBGEMD05UUk5PLTk4MzE2MzMyNzETMBEGA1UECgwKQnV5\n" +
            "cGFzcyBBUzEwMC4GA1UEAwwnQnV5cGFzcyBDbGFzcyAzIFRlc3Q0IENBIEcyIFNU\n" +
            "IEJ1c2luZXNzMB4XDTIzMDUwNTEzNTMxOFoXDTI2MDUwNTIxNTkwMFowfTELMAkG\n" +
            "A1UEBhMCTk8xJDAiBgNVBAoMG0RJR0lUQUxJU0VSSU5HU0RJUkVLVE9SQVRFVDEM\n" +
            "MAoGA1UECwwDRk1UMSAwHgYDVQQDDBdEaWdkaXItdGVzdC1lRm9ybWlkbGluZzEY\n" +
            "MBYGA1UEYQwPTlRSTk8tOTkxODI1ODI3MIIBojANBgkqhkiG9w0BAQEFAAOCAY8A\n" +
            "MIIBigKCAYEArrsStigatgR/8Mc95kIZQC4+bQOoHPERMjwRoCJ4knMgw0WWceZc\n" +
            "T83KcafLjohis/qplFcc5R/s8mnE8pTFWOjaaEJPXjkfYdpy2m1trq0LyqALZmuK\n" +
            "F1giL8+0hmiUx3ZFkx7gPbC2sBsK5lFnVzZXqzKKZDLBZvPxQAgjqWqI7REQrSxV\n" +
            "5hy302IP8GEtTyA2xyVhBPeSDU/WUiIwczLWdHQnrl5lUz/SY+EYlyql0iZc5cd+\n" +
            "15eyuTb8ene2OWZ/hAghpRGegGP9aNhaghs1ydo1fBux3b581vSkzpkaGDSBjDcq\n" +
            "aHoH9WYVlximb7rmaLu7cvR+1FwLWizpdB8CFeV9TTVwThE4pwHX2Mv12mpLd6mu\n" +
            "6f49op08fKB532m2bKP9wpoTHGBkN6Cy8EcvqY3BmF4f92rwpRPy7mxrzXdn+8mh\n" +
            "iyRhYLsbvQuIlouGqad0PTkeRe1Db0UarA46cFZkpFFXNjlZyo8TNjXd9/oTDA/c\n" +
            "ZfkReXt/4FZJAgMBAAGjggFnMIIBYzAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFKf+\n" +
            "u2xZiK10LkZeemj50bu/z7aLMB0GA1UdDgQWBBQaIxxqzdJNzDUCHgGbfzE+wIGN\n" +
            "1zAOBgNVHQ8BAf8EBAMCBaAwHwYDVR0gBBgwFjAKBghghEIBGgEDAjAIBgYEAI96\n" +
            "AQEwQQYDVR0fBDowODA2oDSgMoYwaHR0cDovL2NybC50ZXN0NC5idXlwYXNzY2Eu\n" +
            "Y29tL0JQQ2wzQ2FHMlNUQlMuY3JsMHsGCCsGAQUFBwEBBG8wbTAtBggrBgEFBQcw\n" +
            "AYYhaHR0cDovL29jc3Bicy50ZXN0NC5idXlwYXNzY2EuY29tMDwGCCsGAQUFBzAC\n" +
            "hjBodHRwOi8vY3J0LnRlc3Q0LmJ1eXBhc3NjYS5jb20vQlBDbDNDYUcyU1RCUy5j\n" +
            "ZXIwJQYIKwYBBQUHAQMEGTAXMBUGCCsGAQUFBwsCMAkGBwQAi+xJAQIwDQYJKoZI\n" +
            "hvcNAQELBQADggIBABdfCXdNgzGI1IHsb2WtmKXCOu6owanWKBBmAUQiTdMGPCTi\n" +
            "pDtmS1IFCYq1qT658cfuX+5Jv9ntCmEXP8BbO79p8f8QrypFoigAR2zRXhLEX8fo\n" +
            "jxIrdWg/0ChowyHNZmU3R3hd4rjDc4Py6p4l4YaRHeWUP7YUHy4zqe0LXBPk8vpr\n" +
            "9JKIQLqZ3CFFkHBVp2x7CJLelK2N/dZGqSWBzLFJXbyswaxgPHWbp9UFoZyI99li\n" +
            "nAvOJQGYmPYxhXVij8C9TUdXeeIcLqpXIZb/Y7PyGIZeT3o3Cw2LguBtR5citrl4\n" +
            "NsoMWkzGzN0aOHGc4su0JxKKXAE+jflbp1nsCb0mXNiGGRCFlvnNitfhQUGSuiIh\n" +
            "Sr8WsbYUniPXlJ4uTmOe9JWpBoy1wWQ0aQAAh30k1dUiT3kAfPmzbig95tBDL8/D\n" +
            "G4Q9ZPbpRFwdOsXK2C3zFTqIFR01bktfayZIEI58xOgRv2Oz7HY7WQxS6ajw6W6z\n" +
            "RbWvy+LMx8i+v4V5c4COhbC6PdRS0+q69TrAIS/FflErS//vEHky/0MTX0mgZ0t6\n" +
            "o2U8PPs9w33n9KrQNwGaKDMNNaAI1Gyw35VzzUS1E/nYNJIOef7lQhQgAH7ZmmMG\n" +
            "fPvbDZMhJCYJON3Qapgmbku6zmH1DB43E8EuaMOlDjESNEJMITi5IY63USnE\n" +
            "-----END CERTIFICATE-----";

        String pem2 = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGkjCCBHqgAwIBAgIKTVM0Fs8tjvuXVzANBgkqhkiG9w0BAQ0FADBqMQswCQYD\n" +
            "VQQGEwJOTzEYMBYGA1UEYQwPTlRSTk8tOTgzMTYzMzI3MRMwEQYDVQQKDApCdXlw\n" +
            "YXNzIEFTMSwwKgYDVQQDDCNCdXlwYXNzIENsYXNzIDMgVGVzdDQgUm9vdCBDQSBH\n" +
            "MiBTVDAeFw0yMDExMDQxOTQ5MjBaFw00MDExMDQxOTQ5MjBaMG4xCzAJBgNVBAYT\n" +
            "Ak5PMRgwFgYDVQRhDA9OVFJOTy05ODMxNjMzMjcxEzARBgNVBAoMCkJ1eXBhc3Mg\n" +
            "QVMxMDAuBgNVBAMMJ0J1eXBhc3MgQ2xhc3MgMyBUZXN0NCBDQSBHMiBTVCBCdXNp\n" +
            "bmVzczCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK5BcdxViE38nt2s\n" +
            "0UVltpmHeiRdb0ta2pbUF11DvzZ2mkZTRYXRcxUw6FqbzzrtmW/9dRllI3P7CWmd\n" +
            "n2n1cqiIqpCXricg47nchsUyMFO72O6OzKjbekjWW9tt5iTEj2SWlW4XJcjGaKM2\n" +
            "V6krIEXlpEQ14e0yMP9MDu9N4+nRVYjkMPBTBkCGFj1WHgbFzgiphKl9o1bFCwI1\n" +
            "Bxgi0udiRNjGDDAcIODqsWDgAdA5uJaVLAOexgNlj4r2XrYyDsXXAbQsvxntCdix\n" +
            "uQrKBVHwhMSfDmiCxZIuL0O0W9d+24SwkUzhivVXcAHe90PvIdeXEDeSUJj6UhLU\n" +
            "94ENqwry3AOHgmf0Vo8O8oDTpXlooFY/+G954i8ZH/mVDRQtkYcBd/axpBiQ3B2D\n" +
            "9HJSUq3anUILiyV1s74RHV+euFaH36C19AOV60R8fvtBS2lUly1yuEc60IGhHaGR\n" +
            "5tve/YHEKfrbewiglTaa5bEruw9+v9ZxJoTPGjFCgtL4sNtS1jgRAo0gQy2vKieW\n" +
            "Z7U1ArwhsrYNfhPYXfxmN7MQv7/DGn4Ry4Tn6jGGX5r/BwlBlaP/e4pWPKboy7pn\n" +
            "YcAxPA2EJ48UQO9+CWQVZNVM+0h9oz5jsOxUK3Lo7IQpkCe1b9Q7eSJaN/hKagwn\n" +
            "+wvoLD7Kqh+oQW9rJdinQAZWZJmdAgMBAAGjggE0MIIBMDAPBgNVHRMBAf8EBTAD\n" +
            "AQH/MB8GA1UdIwQYMBaAFFCz+T2gxyZlm9qoh2/pIj2iXms/MB0GA1UdDgQWBBSn\n" +
            "/rtsWYitdC5GXnpo+dG7v8+2izAOBgNVHQ8BAf8EBAMCAQYwEQYDVR0gBAowCDAG\n" +
            "BgRVHSAAMEMGA1UdHwQ8MDowOKA2oDSGMmh0dHA6Ly9jcmwudGVzdDQuYnV5cGFz\n" +
            "c2NhLmNvbS9CUENsM1Jvb3RDYUcyU1QuY3JsME4GCCsGAQUFBwEBBEIwQDA+Bggr\n" +
            "BgEFBQcwAoYyaHR0cDovL2NydC50ZXN0NC5idXlwYXNzY2EuY29tL0JQQ2wzUm9v\n" +
            "dENhRzJTVC5jZXIwJQYIKwYBBQUHAQMEGTAXMBUGCCsGAQUFBwsCMAkGBwQAi+xJ\n" +
            "AQIwDQYJKoZIhvcNAQENBQADggIBAE8kOiWPnCo33bI1pAAFiBAkzv8+7jnZ5Bx5\n" +
            "rdq9pVZiTEXQfSuygwl4Tofw3ldUIVHa0E0YE96TBinDftSJQzefyQXAaK8I3Ax0\n" +
            "vVOkcVNGrwzJjcK7dSCcxL0rldXpqf2Yc+sMHpVPNtehgEyyqEYQFEJuoM9mJJGe\n" +
            "iXrTCLZez1SzKakbnljs9kLGlgLKnN/Vej/ezyXVNLSGuRNmhv9XuaG+kq61ATes\n" +
            "UYqZbsDK/zNXN/ArpxXor5fZbcXoDOf/yo+j0iTKv2Osw91GBwUKdhx/IygBBveF\n" +
            "ywsowd+GYZpSZZl5s9UAq64A98BZ3atwocVgT5gwWgNLjmSLXiWt42dRMdUl5qMI\n" +
            "1y6+xXUcRbzPnobUeX7ZVR8b4L7URDnLq+ldTVvhJHZ/ejOMICx9zTYkzt9urJbb\n" +
            "l4TE8wEh5ofebdpipucba0NEFGUTbPopCVK56BXsAyZfT6cowFIeLKVsL2C7cY/e\n" +
            "fhZs0SIgrFoVR0ETuIh4/U6Mw4PmigP61HaxS/8OVDBMHc1uZT7vefhv16Hi4Vub\n" +
            "hb1hbfNEewNG2xkqhWapFIvjVTEXTp12Jn4Hnugj/fCIE7aHYhMXmU3C651Acu7A\n" +
            "XGHXWY8NUg/9Yhkp/n9AVeiL2SYqnQqG/pKe5iUNixNyoyqzdOtJO4kDNykyOJ4n\n" +
            "czBqUwOW\n" +
            "-----END CERTIFICATE-----";

        String pem3 = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFwTCCA6mgAwIBAgIKcDEIzY8CWLQVtzANBgkqhkiG9w0BAQ0FADBqMQswCQYD\n" +
            "VQQGEwJOTzEYMBYGA1UEYQwPTlRSTk8tOTgzMTYzMzI3MRMwEQYDVQQKDApCdXlw\n" +
            "YXNzIEFTMSwwKgYDVQQDDCNCdXlwYXNzIENsYXNzIDMgVGVzdDQgUm9vdCBDQSBH\n" +
            "MiBTVDAeFw0yMDExMDUxNzMwMThaFw00NTExMDUxNzMwMThaMGoxCzAJBgNVBAYT\n" +
            "Ak5PMRgwFgYDVQRhDA9OVFJOTy05ODMxNjMzMjcxEzARBgNVBAoMCkJ1eXBhc3Mg\n" +
            "QVMxLDAqBgNVBAMMI0J1eXBhc3MgQ2xhc3MgMyBUZXN0NCBSb290IENBIEcyIFNU\n" +
            "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAp5iPrsAqUNhggKgFKR9z\n" +
            "ummTTLthRtAwQ4FhqGJFavswFQJKHR27duFq3ctwdyr3JTUORpusJz8laX0PLH9m\n" +
            "6yKpP4KUSEjPe9Ws2Qg/iEyt75TiaEu+xplZW3F7Fp09H9/WZOxhGm2z5gt/YDto\n" +
            "t4mBbHbjh/MMj/z1mplRwyBIhOFgttwg9jUIlcmkJ0Udg+M+vaBfBY4xL7Q1m6VO\n" +
            "Z5/BdUB6doeqWa3yBCEQekbWvaG6zeEXk2JQ/2OTQ7gKXNObgsuSO1jYphHG+i4+\n" +
            "2H/RgNINTQMGf4RrvbO5A0VurWoibFL8H0IoC+DzRHgTddswz8iV3aO9BK57HjjH\n" +
            "MgbzqR1m+Z1JhUn6gUo6uE4heHW00wlgnbVxGNl7VrNdPGFED/gxtzYDKHcyeJxc\n" +
            "7EPpPasN+aWEGcooq6CE+GP/KGD3Wwrh316ydh6dfbtp50pZabZ+2EXc303MyVly\n" +
            "ErrSUvHn5Hxv2Ekq1QRPY4aD/mZx3yl222oYXUCxHX5F1nQTdabDz70EtdQGUjRz\n" +
            "v9aqZUZpKM0RU+kb2E2MTVb2Q7lBRbefDLl0pJA8R6IN2LOE/ICDZpD0tihB71rB\n" +
            "TEABFoBPDokGioolqJ+F8l2w8B7HNg4sVxu872PDyGmwa2oR+1b9tIBCkjx681eu\n" +
            "WYdzxM3gZ2JCK5WovlEagm8CAwEAAaNpMGcwDwYDVR0TAQH/BAUwAwEB/zAdBgNV\n" +
            "HQ4EFgQUULP5PaDHJmWb2qiHb+kiPaJeaz8wDgYDVR0PAQH/BAQDAgEGMCUGCCsG\n" +
            "AQUFBwEDBBkwFzAVBggrBgEFBQcLAjAJBgcEAIvsSQECMA0GCSqGSIb3DQEBDQUA\n" +
            "A4ICAQAggOvmP9+CKPK1ghJfGShHXnx52vy9X8h81ODnvpvOJfbb7iB1+R//hUvt\n" +
            "fcAB0DnRSSUEvXE0bkym2a85VsR2SizFrcHOSMBc5ZZHKB61V+s3F7cFLtk1qetc\n" +
            "VsEY9jo7pa7elHB56jILHh4ivfVpXlrNifwtwVMGNH7fNkrVz1xLfeaYhgv63pdU\n" +
            "L+XsZjbB4+EkYU5C3ZERsD5nnf0+g/14W17ktUMbuddwl4pMAcPuVd+aPD+LFfRC\n" +
            "5mLB+/E/rZSutYPwGo31cDJAeLpm1D5t8WWZxMfGP77dHoGzEWDVB5od3e1S5cIh\n" +
            "bLOUIZ2az00SzJ3i4HNj6GWDD4p8HzvBFZ3DA6oyC+ZyCpwMAQb+5JZbv5cOxlr4\n" +
            "JqxtAPrs1oGO9H7qc0isvvWcTiP6F/WqvYoyQZWl28O7oqSv/GAmK2g/oXDQ+mjz\n" +
            "QS6MIQGNdfb9ZN+i7E/XMCZ4N28Wk0V7jNpZz7R2O1FGJUfGn1z1ZByJQC+Og6dK\n" +
            "Qvwpj2E41qE1pYEYy4hpVxudLpuCUGSxkC0CcEV+7xL+BW04GpZn5TxAqN44/FpP\n" +
            "CCW73Apk3cYd8LvfzKme5Z26jDNwp2ZDqru1zsQ5Mky/3F9ALB8CCu+5LZbmLHzs\n" +
            "CDJoMnYDsfs0SweN2BqsZYi8eJcAErw76aunuR0F4g51KKlJtQ==\n" +
            "-----END CERTIFICATE-----";

        String pemPrivateKey = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIG/wIBADANBgkqhkiG9w0BAQEFAASCBukwggblAgEAAoIBgQCuuxK2KBq2BH/w\n" +
            "xz3mQhlALj5tA6gc8REyPBGgIniScyDDRZZx5lxPzcpxp8uOiGKz+qmUVxzlH+zy\n" +
            "acTylMVY6NpoQk9eOR9h2nLabW2urQvKoAtma4oXWCIvz7SGaJTHdkWTHuA9sLaw\n" +
            "GwrmUWdXNlerMopkMsFm8/FACCOpaojtERCtLFXmHLfTYg/wYS1PIDbHJWEE95IN\n" +
            "T9ZSIjBzMtZ0dCeuXmVTP9Jj4RiXKqXSJlzlx37Xl7K5Nvx6d7Y5Zn+ECCGlEZ6A\n" +
            "Y/1o2FqCGzXJ2jV8G7HdvnzW9KTOmRoYNIGMNypoegf1ZhWXGKZvuuZou7ty9H7U\n" +
            "XAtaLOl0HwIV5X1NNXBOETinAdfYy/Xaakt3qa7p/j2inTx8oHnfabZso/3CmhMc\n" +
            "YGQ3oLLwRy+pjcGYXh/3avClE/LubGvNd2f7yaGLJGFguxu9C4iWi4app3Q9OR5F\n" +
            "7UNvRRqsDjpwVmSkUVc2OVnKjxM2Nd33+hMMD9xl+RF5e3/gVkkCAwEAAQKCAYEA\n" +
            "klbLTRQ5isgp/xZfmUEytLYGQncVjWe450G34U39fWo+vkzBu5MbLRtFufq2WSGQ\n" +
            "yPvemwZ6hMtAP6al9CT1Q4mBomAm6NselW3HU3jiq7DydzmjUpwug26k8zYaE5fz\n" +
            "Qzqi+pyZYhFmcAe2ET+MNVamBhPGJDT+FaOpId+vPmyjUSlBtkeziY3zx24Ru1ko\n" +
            "AWURe6iqoOZXxB5heVh5C4JAajf+I+ObKSYCpvQXDEQDrQXEOA3lBdtfLBAJVOh8\n" +
            "q0CxV42SowgTtOo76oAGgq+FBZ4d3ugL4GNulmyHHB/R5Ai/Xcf7aeA55CfhkRIc\n" +
            "gAWRUxOmQ/PretGln8fIwU9FDAeu2gtIk6iJ1UQsYPCIOjXnMqYOZNgjigOS6P/x\n" +
            "zFWKJAdHttuzQO2ps2v9ieceGgrcXOuVX2JAEeME2BqXhTFue9Htdes4vfbElEWX\n" +
            "09NlbpxlYm2CHwU6hG9sPCh0HspeUAwNxsNIVRLzRWVAqxSlLHnz3CgbEYHPQBWh\n" +
            "AoHBANR8rVt/TsPf86iN2jOXb3WLTYb4zqXeScYK7GgW/zAwUpJNukG1mkAw8Gum\n" +
            "HwRFm+0uOnhOn0GKUxcyO4gkLzd/niQQdbW67p44ek7+JRUlrjDZAEU6HSU6pcYq\n" +
            "9uYIQju3i14R4gLLHoPnh2m5RSte5O90sUVNf7vlG4FPRxTcr1WFZa+T6zROzhep\n" +
            "gBSL5tVBc5NSPCoI6RYDvG8YNr8GStVt3m8nksEO/QEhvKDfXGY1zKTZ82ciwKRs\n" +
            "4ca4owKBwQDSgxQgG2BdgY8YxQHds1AnI0FNRqaxMswDVQT8+VgeNYV/4cNEpq7u\n" +
            "bzUkxj4FsSKtrETXlx4ZCiGro9Izp8ANbCGDZyGgr0qU3Vg664yui48QvrJASqAV\n" +
            "QOxwK4q+d9q/wgh2B/95pTFuMWWpMw5yKglGkwuWOjfuQCxINT+hJdh0LB/cpNwZ\n" +
            "mf5nVNVBGI5Yk2y9TA8S/P53VMF/qZ9UQJyzWtzfe6QOs2o3tQN8tZZGtPA+ZRvS\n" +
            "3V8T+vNQCCMCgcEAqGLRAmQlqAWuUpFvLFlCg5TL8Dz1je5U6jM32tj2i1qP5M7h\n" +
            "3jaqsVnW/IfdpoX/JfrvbO1pOQgEBqrREHcUT9e7G3tLvKMRe6LoEfdGrbZ5js44\n" +
            "b6O4+wq52gusMuiyjB5MvFRRlcMfQIpW/gpK7S67H6OK44+hvOW3YUbQ3pBwBca7\n" +
            "qWMADiixG4FANYgUekTu8P1HHkyHYeG+Dw/SOpki7nWtwspKeuGEz8PLdnTrBTh3\n" +
            "HuB1CfM5VFPQq6kfAoHBAIDPDfB7b+KnVQz99x9WChR4oQuM0JCamXTsf+nfFIkf\n" +
            "jLlsRhRqvZ1N1MHUv7yx35tPTqxwXk1KAsJIsmVDHuPXp0YFQ/FTcRpXuhYLbYCc\n" +
            "CoVfeiiWwQ9gM4yLSBE1u1ccfhri1/LyHqXjeeYnRI6cpCJX9X4Nz41sHOxuEKol\n" +
            "QyYKndBf3AGaVa2angZ/5RHT23SU5qBix73y0ZexDbn68ydX0NC6ke8g3zyI+UJE\n" +
            "xlgrfNlg+r5zECtkoZ716wKBwHb0rhe9CAAIJugRfVYwYwoK2xnsRJ27g5QAYEXM\n" +
            "dBwmFplWH025zpyjA6F7WR4sQgxjw8pRQIyT9CgCu4Wwgp3vmcKPxGCNDFNajymc\n" +
            "/edsT/RErJzgrLC6CwzrtFyP2MA/cz6jkFih7upJSlitHa0DXYHADAbi0VZr0+db\n" +
            "OETjWcC+LYTYgBIRqNWqOqJO3Ws6P4GgZuriSJ5+Ib5/qp7VxdYPhYKfSw2fCumC\n" +
            "YsXBXfDdjcSHIqzwMUE7v8s7Bw==\n" +
            "-----END PRIVATE KEY-----";

        // Henter autorisasjonsbevis frå Maskinporten
        // Forutsetning: Klient med ønska scopes er oppretta i Maskinporten
        JWK jwk = JWK.parseFromPEMEncodedObjects(pemPrivateKey);
        List<Base64> x5c = JWK.parseFromPEMEncodedX509Cert(pem1 + "\n" + pem2 + "\n" + pem3).getX509CertChain();
        RSAKey rsaPrivateKey = jwk.toRSAKey();
        JWSSigner jwsSigner = new RSASSASigner(rsaPrivateKey);
        Date issueTime = new Date();
        Date expirationTime = new Date(issueTime.getTime() + 60 * 1000);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .issuer("a63cac91-3210-4c35-b961-5c7bf122345c")
            .issueTime(issueTime)
            .expirationTime(expirationTime)
            .audience("https://test.maskinporten.no/")
            .claim("scope", scopes)
            .build();
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .x509CertChain(x5c)
            .build();
        SignedJWT signedJwt = new SignedJWT(
            jwsHeader,
            jwtClaimsSet
        );
        signedJwt.sign(jwsSigner);
        String jwtString = signedJwt.serialize();
        System.out.println(jwtString);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://test.maskinporten.no/token"))
            .header("Content-type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(
                "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + jwtString
            ))
            .build();
        HttpResponse httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println(httpResponse.statusCode());
        System.out.println(httpResponse.body());
        String body = (String) httpResponse.body();
        String marker = "\"access_token\":\"";
        int beginIndex = body.indexOf(marker) + marker.length();
        int endIndex = body.indexOf("\"", beginIndex);
        String accessToken = body.substring(beginIndex, endIndex);
        System.out.println(accessToken);

        // Veksler inn autorisasjonsbevis frå Maskinporten med autorisasjonsbevis frå Altinn - kan hende blir unødvendig litt fram i tid
        HttpRequest httpRequest2 = HttpRequest.newBuilder()
            //.uri(URI.create("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten?test=true")) // blir organisasjonen ttd
            .uri(URI.create("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten")) // blir organisasjonen digdir
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
        HttpResponse httpResponse2 = httpClient.send(httpRequest2, HttpResponse.BodyHandlers.ofString());
        System.out.println(httpResponse2.statusCode());
        System.out.println(httpResponse2.body());
        String accessToken2 = (String) httpResponse2.body();
        System.out.println(accessToken2);
        return accessToken2;
    }

}
