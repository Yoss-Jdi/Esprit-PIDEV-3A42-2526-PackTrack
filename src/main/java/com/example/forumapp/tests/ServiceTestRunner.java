package com.example.forumapp.tests;

import com.example.forumapp.entities.User;
import com.example.forumapp.entities.UserRole;
import com.example.forumapp.utils.PasswordUtils;

public class ServiceTestRunner {

    public static void main(String[] args) {
        System.out.println("=== Forum — Tests unitaires ===\n");
        int passed = 0;
        int failed = 0;

        // Test 1 : creation User minimal
        try {
            User user = new User();
            user.setId(1);
            user.setNom("Test User");
            user.setEmail("test@test.com");
            user.setRole(UserRole.USER);
            assert user.getId() == 1;
            assert "Test User".equals(user.getNom());
            assert "test@test.com".equals(user.getEmail());
            assert user.getRole() == UserRole.USER;
            assert !user.isAdmin();
            System.out.println("[OK] User : creation et getters");
            passed++;
        } catch (AssertionError e) {
            System.out.println("[FAIL] User : creation et getters — " + e.getMessage());
            failed++;
        }

        // Test 2 : User admin
        try {
            User admin = new User();
            admin.setRole(UserRole.ADMIN);
            assert admin.isAdmin();
            System.out.println("[OK] User : detection role ADMIN");
            passed++;
        } catch (AssertionError e) {
            System.out.println("[FAIL] User : detection role ADMIN — " + e.getMessage());
            failed++;
        }

        // Test 3 : User toString
        try {
            User user = new User();
            user.setNom("Alice");
            user.setRole(UserRole.USER);
            assert "Alice (USER)".equals(user.toString());
            System.out.println("[OK] User : toString");
            passed++;
        } catch (AssertionError e) {
            System.out.println("[FAIL] User : toString — " + e.getMessage());
            failed++;
        }

        // Test 4 : PasswordUtils hash
        try {
            String hash = PasswordUtils.hash("admin123");
            assert hash != null && !hash.isBlank();
            assert hash.length() == 64; // SHA-256 = 64 hex chars
            System.out.println("[OK] PasswordUtils : hash produit 64 caracteres hex");
            passed++;
        } catch (AssertionError e) {
            System.out.println("[FAIL] PasswordUtils : hash — " + e.getMessage());
            failed++;
        }

        // Test 5 : PasswordUtils matches
        try {
            String hash = PasswordUtils.hash("secret");
            assert PasswordUtils.matches("secret", hash);
            assert !PasswordUtils.matches("wrong", hash);
            System.out.println("[OK] PasswordUtils : matches correct/incorrect");
            passed++;
        } catch (AssertionError e) {
            System.out.println("[FAIL] PasswordUtils : matches — " + e.getMessage());
            failed++;
        }

        // Test 6 : PasswordUtils deterministic
        try {
            String h1 = PasswordUtils.hash("test");
            String h2 = PasswordUtils.hash("test");
            assert h1.equals(h2);
            System.out.println("[OK] PasswordUtils : hash deterministe");
            passed++;
        } catch (AssertionError e) {
            System.out.println("[FAIL] PasswordUtils : hash deterministe — " + e.getMessage());
            failed++;
        }

        // Test 7 : hash connu admin123
        try {
            String expected = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
            assert PasswordUtils.matches("admin123", expected);
            System.out.println("[OK] PasswordUtils : hash connu admin123");
            passed++;
        } catch (AssertionError e) {
            System.out.println("[FAIL] PasswordUtils : hash connu admin123 — " + e.getMessage());
            failed++;
        }

        // Test 8 : hash connu user123
        try {
            String expected = "e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446";
            assert PasswordUtils.matches("user123", expected);
            System.out.println("[OK] PasswordUtils : hash connu user123");
            passed++;
        } catch (AssertionError e) {
            System.out.println("[FAIL] PasswordUtils : hash connu user123 — " + e.getMessage());
            failed++;
        }

        System.out.println("\n=== Resultats : " + passed + " OK, " + failed + " FAIL ===");
        if (failed > 0) System.exit(1);
    }
}
