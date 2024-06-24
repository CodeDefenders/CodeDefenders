package org.codedefenders.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderProvider {
    @Produces
    @ApplicationScoped
    public static PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
