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

import java.time.LocalDateTime;
import java.util.List;

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
        User user = User.builder()
            .email(req.email())
            .username(req.username())
            .passwordHash(passwordEncoder.encode(req.password()))
            .build();
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new BadCredentialsException("メールアドレスまたはパスワードが正しくありません"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("メールアドレスまたはパスワードが正しくありません");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        List<User> users = userRepository.findAll();
        User user = users.stream()
            .filter(u -> u.getRefreshTokenHash() != null
                && passwordEncoder.matches(rawRefreshToken, u.getRefreshTokenHash()))
            .findFirst()
            .orElseThrow(() -> new InvalidTokenException("リフレッシュトークンが無効です"));

        if (user.getRefreshTokenExpiresAt() == null
                || user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("リフレッシュトークンの有効期限が切れています");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        List<User> users = userRepository.findAll();
        users.stream()
            .filter(u -> u.getRefreshTokenHash() != null
                && passwordEncoder.matches(rawRefreshToken, u.getRefreshTokenHash()))
            .findFirst()
            .ifPresent(u -> {
                u.setRefreshTokenHash(null);
                u.setRefreshTokenExpiresAt(null);
                userRepository.save(u);
            });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateToken(user.getId());
        String rawRefreshToken = jwtUtil.generateRefreshToken();
        long refreshMs = jwtUtil.getRefreshExpirationMs();

        user.setRefreshTokenHash(passwordEncoder.encode(rawRefreshToken));
        user.setRefreshTokenExpiresAt(
            LocalDateTime.now().plusSeconds(refreshMs / 1000));
        userRepository.save(user);

        return new AuthResponse(
            accessToken,
            rawRefreshToken,
            jwtUtil.getExpirationMs() / 1000,
            toUserResponse(user)
        );
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
