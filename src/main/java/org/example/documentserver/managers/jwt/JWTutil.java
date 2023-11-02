
package org.example.documentserver.managers.jwt;

import org.example.PayloadConvert;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JWTutil {
    @Value("${files.docservice.secret}")
    private String tokenSecret;
    public String createToken(final PayloadConvert payload) {
        try {
            // build a HMAC signer using a SHA-256 hash
            Signer signer = HMACSigner.newSHA256Signer(this.tokenSecret);
            JWT jwt = new JWT();
            jwt.addClaim("async", payload.isAsync());
            jwt.addClaim("fileType", payload.getFileType());
            jwt.addClaim("key", payload.getKey());
            jwt.addClaim("outputType", payload.getOutputType());
            jwt.addClaim("title", payload.getTitle());
            jwt.addClaim("url", payload.getUrl());
            return JWT.getEncoder().encode(jwt, signer);
        } catch (Exception e) {
            return "";
        }
    }
}
