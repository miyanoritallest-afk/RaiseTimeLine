package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.LoginRequest;
import com.raisetimeline.backend.dto.request.RegisterRequest;
import com.raisetimeline.backend.dto.response.AuthResponse;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.DuplicateResourceException;
import com.raisetimeline.backend.repository.UserRepository;
import com.raisetimeline.backend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    @Test
    void register_success() {
        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        User saved = User.builder().id(1L).email("test@example.com")
            .username("testuser").passwordHash("hashed").build();
        given(userRepository.save(any(User.class))).willReturn(saved);
        given(jwtUtil.generateToken(1L)).willReturn("token123");

        AuthResponse res = authService.register(
            new RegisterRequest("test@example.com", "testuser", "password1"));

        assertThat(res.token()).isEqualTo("token123");
        assertThat(res.user().username()).isEqualTo("testuser");
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() ->
            authService.register(new RegisterRequest("dup@example.com", "u", "password1"))
        ).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void login_success() {
        User user = User.builder().id(1L).email("login@example.com")
            .username("loginuser").passwordHash("hashed").build();
        given(userRepository.findByEmail("login@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1", "hashed")).willReturn(true);
        given(jwtUtil.generateToken(1L)).willReturn("token456");

        AuthResponse res = authService.login(
            new LoginRequest("login@example.com", "password1"));

        assertThat(res.token()).isEqualTo("token456");
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        User user = User.builder().id(1L).email("bad@example.com")
            .username("baduser").passwordHash("hashed").build();
        given(userRepository.findByEmail("bad@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

        assertThatThrownBy(() ->
            authService.login(new LoginRequest("bad@example.com", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_userNotFound_throwsBadCredentials() {
        given(userRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            authService.login(new LoginRequest("none@example.com", "pass"))
        ).isInstanceOf(BadCredentialsException.class);
    }
}
