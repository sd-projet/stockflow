
package com.stockflow.api;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        Console console = System.console();

        if (console == null) {
            System.err.println(
                "Lance ce programme depuis un terminal qui accepte la saisie masquée."
            );
            return;
        }

        char[] password = console.readPassword("Nouveau mot de passe : ");

        if (password == null || password.length == 0) {
            System.err.println("Le mot de passe ne peut pas être vide.");
            return;
        }

        try {
            String hash = new BCryptPasswordEncoder().encode(
                new String(password)
            );
            System.out.println("Hash BCrypt : " + hash);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
}
