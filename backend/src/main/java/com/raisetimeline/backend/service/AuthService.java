package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.LoginRequest;
import com.raisetimeline.backend.dto.request.RegisterRequest;
import com.raisetimeline.backend.dto.response.AuthResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.DuplicateResourceException;
import com.raisetimeline.backend.repository.UserRepository;
import com.raisetimeline.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        String token = jwtUtil.generateToken(user.getId());
        return new AuthResponse(token, toUserResponse(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new BadCredentialsException("メールアドレスまたはパスワードが正しくありません"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("メールアドレスまたはパスワードが正しくありません");
        }
        String token = jwtUtil.generateToken(user.getId());
        return new AuthResponse(token, toUserResponse(user));
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
