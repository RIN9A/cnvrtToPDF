
package org.example.documentserver.managers.jwt;

import org.example.PayloadConvert;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JWTutil {

    private String tokenSecret = "duhudsysy74834gdt8"; //change on secret token from Docker Server
    public String createToken(final PayloadConvert payload) {
        try {
            // build a HMAC signer using a SHA-256 hash
            Signer signer = HMACSigner.newSHA256Signer(tokenSecret);
            JWT jwt = new JWT();
            jwt.addClaim("async", payload.isAsync());
            jwt.addClaim("filetype", payload.getFiletype());
            jwt.addClaim("key", payload.getKey());
            jwt.addClaim("outputtype", payload.getOutputtype());
            jwt.addClaim("title", payload.getTitle());
            jwt.addClaim("url", payload.getUrl());
            return JWT.getEncoder().encode(jwt, signer);
        } catch (Exception e) {
            return "";
        }
    }

    public String createToken(final Map<String, Object> payloadClaims) {
        try {

            Signer signer = HMACSigner.newSHA256Signer(tokenSecret);
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet()) {  // run through all the keys from the payload
                jwt.addClaim(key, payloadClaims.get(key));  // and write each claim to the jwt
            }
            return JWT.getEncoder().encode(jwt, signer);  // sign and encode the JWT to a JSON string representation
        } catch (Exception e) {
            return "";
        }
    }
}
