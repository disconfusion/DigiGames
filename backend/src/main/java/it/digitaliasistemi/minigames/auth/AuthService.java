package it.digitaliasistemi.minigames.auth;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.util.ModularCrypt;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    public String issueToken(String username, String displayName, String role) {
        return Jwt.issuer("minigames")
                .subject(username)
                .upn(username)
                .claim("displayName", displayName)
                .groups(Set.of(role))
                .expiresIn(Duration.ofHours(12))
                .sign();
    }

    public boolean verifyPassword(String plain, String bcryptHash) {
        try {
            var provider = new WildFlyElytronPasswordProvider();
            var decoded = (BCryptPassword) ModularCrypt.decode(bcryptHash);
            var factory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT, provider);
            var restored = factory.translate(decoded);
            return factory.verify(restored, plain.toCharArray());
        } catch (Exception e) {
            return false;
        }
    }
}
