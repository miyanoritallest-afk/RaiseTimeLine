package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.LoginRequest;
import com.raisetimeline.backend.dto.request.RegisterRequest;
import com.raisetimeline.backend.dto.response.AuthResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.DuplicateResourceException;
import com.raisetimeline.backend.exception.InvalidTokenException;
import com.raisetimeline.backend.repository.UserRepository;
import com.raisetimeline.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("このメールアドレスは既に登録されています");
        }
        String rawRefreshToken = jwtUtil.generateRefreshToken();
        User user = User.builder()
            .email(req.email())
            .username(req.username())
            .passwordHash(passwordEncoder.encode(req.password()))
            .refreshTokenPrefix(extractPrefix(rawRefreshToken))
            .refreshTokenHash(hashToken(rawRefreshToken))
            .refreshTokenExpiresAt(expiresAt())
            .build();
        user = userRepository.save(user);
        return toAuthResponse(user, rawRefreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new BadCredentialsException("メールアドレスまたはパスワードが正しくありません"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("メールアドレスまたはパスワードが正しくありません");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        User user = findByRefreshToken(rawRefreshToken);
        if (user.getRefreshTokenExpiresAt() == null
                || user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("リフレッシュトークンの有効期限が切れています");
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        userRepository.findByRefreshTokenPrefix(extractPrefix(rawRefreshToken))
            .stream()
            .filter(u -> u.getRefreshTokenHash() != null
                && hashToken(rawRefreshToken).equals(u.getRefreshTokenHash()))
            .findFirst()
            .ifPresent(u -> {
                u.setRefreshTokenPrefix(null);
                u.setRefreshTokenHash(null);
                u.setRefreshTokenExpiresAt(null);
                userRepository.save(u);
            });
    }

    private User findByRefreshToken(String rawRefreshToken) {
        return userRepository.findByRefreshTokenPrefix(extractPrefix(rawRefreshToken))
            .stream()
            .filter(u -> u.getRefreshTokenHash() != null
                && hashToken(rawRefreshToken).equals(u.getRefreshTokenHash()))
            .findFirst()
            .orElseThrow(() -> new InvalidTokenException("リフレッシュトークンが無効です"));
    }

    private AuthResponse issueTokens(User user) {
        String rawRefreshToken = jwtUtil.generateRefreshToken();
        user.setRefreshTokenPrefix(extractPrefix(rawRefreshToken));
        user.setRefreshTokenHash(hashToken(rawRefreshToken));
        user.setRefreshTokenExpiresAt(expiresAt());
        userRepository.save(user);
        return toAuthResponse(user, rawRefreshToken);
    }

    private AuthResponse toAuthResponse(User user, String rawRefreshToken) {
        return new AuthResponse(
            jwtUtil.generateToken(user.getId()),
            rawRefreshToken,
            jwtUtil.getExpirationMs() / 1000,
            toUserResponse(user)
        );
    }

    private LocalDateTime expiresAt() {
        return LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000);
    }

    private static String extractPrefix(String rawToken) {
        return rawToken.substring(0, 8);
    }

    private static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getBio(),
            user.getAvatarUrl(),
            0L,
            0L,
            false
        );
    }
}
