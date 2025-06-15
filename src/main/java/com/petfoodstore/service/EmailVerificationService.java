package com.petfoodstore.service;

import com.petfoodstore.entity.EmailVerificationToken;
import com.petfoodstore.entity.User;
import com.petfoodstore.repository.EmailVerificationTokenRepository;
import com.petfoodstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EmailVerificationService {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.email-verification.expiration-minutes}")
    private int expirationMinutes;

    @Value("${app.email-verification.token-length}")
    private int tokenLength;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void sendVerificationEmail(User user) {
        // Invalidate any existing tokens
        tokenRepository.findByUserAndUsedFalse(user)
                .ifPresent(token -> {
                    token.setUsed(true);
                    tokenRepository.save(token);
                });

        // Generate new token
        String token = generateToken();
        EmailVerificationToken verificationToken = new EmailVerificationToken(user, token, expirationMinutes);
        tokenRepository.save(verificationToken);

        try {
            emailService.sendVerificationEmail(user.getEmail(), token);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Transactional
    public boolean verifyEmail(String token) {
        return tokenRepository.findByToken(token)
                .filter(verificationToken -> !verificationToken.isExpired() && !verificationToken.isUsed())
                .map(verificationToken -> {
                    User user = verificationToken.getUser();
                    user.setEmailVerified(true);
                    user.setActive(true);
                    userRepository.save(user);

                    verificationToken.setUsed(true);
                    tokenRepository.save(verificationToken);

                    return true;
                })
                .orElse(false);
    }

    private String generateToken() {
        byte[] randomBytes = new byte[tokenLength];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
} 