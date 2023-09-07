package org.codedefenders.auth;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderProvider {
    @Produces
    @ApplicationScoped
    public static PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
