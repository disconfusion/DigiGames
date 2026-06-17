package it.digitaliasistemi.minigames.auth;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.util.ModularCrypt;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

@ApplicationScoped
public class AuthService {

    /** Solo email dei domini aziendali consentiti. */
    private static final Pattern ALLOWED_DOMAIN =
            Pattern.compile("^[a-z0-9._%+-]+@(digitaliasistemi|ascesa)\\.it$");

    public boolean isAllowedEmail(String email) {
        return email != null && ALLOWED_DOMAIN.matcher(email.trim().toLowerCase()).matches();
    }

    /** Emette un JWT firmato (RSA) valido 12 ore. */
    public String issueToken(String email, String displayName, String role) {
        return Jwt.issuer("minigames")
                .subject(email)
                .upn(email)
                .claim("displayName", displayName)
                .groups(Set.of(role))
                .expiresIn(Duration.ofHours(12))
                .sign();
    }

    /** Verifica una password in chiaro contro l'hash bcrypt (Modular Crypt Format). */
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
