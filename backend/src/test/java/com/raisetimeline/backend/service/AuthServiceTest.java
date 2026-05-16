package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.LoginRequest;
import com.raisetimeline.backend.dto.request.RegisterRequest;
import com.raisetimeline.backend.dto.response.AuthResponse;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.DuplicateResourceException;
import com.raisetimeline.backend.exception.InvalidTokenException;
import com.raisetimeline.backend.repository.UserRepository;
import com.raisetimeline.backend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
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

    // ─── register ─────────────────────────────────────────────────────────

    // BB同値分割（有効クラス）: 新規メール → 登録成功
    @Test
    void register_newEmail_success() {
        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        User saved = User.builder().id(1L).email("test@example.com")
            .username("testuser").passwordHash("hashed").build();
        given(userRepository.save(any(User.class))).willReturn(saved);
        given(jwtUtil.generateToken(1L)).willReturn("token123");
        given(jwtUtil.generateRefreshToken()).willReturn("12345678-refresh-token-value");
        given(jwtUtil.getExpirationMs()).willReturn(900000L);
        given(jwtUtil.getRefreshExpirationMs()).willReturn(604800000L);

        AuthResponse res = authService.register(
            new RegisterRequest("test@example.com", "testuser", "password1"));

        assertThat(res.accessToken()).isEqualTo("token123");
        assertThat(res.user().username()).isEqualTo("testuser");
    }

    // BB同値分割（無効クラス）: 重複メール → DuplicateResourceException
    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() ->
            authService.register(new RegisterRequest("dup@example.com", "u", "password1"))
        ).isInstanceOf(DuplicateResourceException.class);
    }

    // ─── login ────────────────────────────────────────────────────────────

    // BB同値分割（有効クラス）: 正しい認証情報 → 成功
    @Test
    void login_validCredentials_success() {
        User user = User.builder().id(1L).email("login@example.com")
            .username("loginuser").passwordHash("hashed").build();
        given(userRepository.findByEmail("login@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1", "hashed")).willReturn(true);
        given(jwtUtil.generateToken(1L)).willReturn("token456");
        given(jwtUtil.generateRefreshToken()).willReturn("12345678-refresh-token-value");
        given(jwtUtil.getExpirationMs()).willReturn(900000L);
        given(jwtUtil.getRefreshExpirationMs()).willReturn(604800000L);

        AuthResponse res = authService.login(new LoginRequest("login@example.com", "password1"));

        assertThat(res.accessToken()).isEqualTo("token456");
    }

    // BB同値分割（無効クラス）: ユーザー未存在 → BadCredentialsException
    @Test
    void login_userNotFound_throwsBadCredentials() {
        given(userRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            authService.login(new LoginRequest("none@example.com", "pass"))
        ).isInstanceOf(BadCredentialsException.class);
    }

    // BB同値分割（無効クラス）: パスワード不一致 → BadCredentialsException
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

    // ─── refresh ──────────────────────────────────────────────────────────

    // WB分岐網羅: prefix一致・hash一致・期限内 → 新トークン発行
    @Test
    void refresh_validToken_notExpired_success() {
        String rawToken = "12345678-abcd-efgh-ijkl-mnopqrstuvwx";
        String prefix = rawToken.substring(0, 8); // "12345678"

        // SHA-256 ハッシュを AuthService と同じ方法で計算
        String hash = sha256(rawToken);

        User user = User.builder().id(1L).email("u@example.com").username("u")
            .passwordHash("hashed")
            .refreshTokenPrefix(prefix)
            .refreshTokenHash(hash)
            .refreshTokenExpiresAt(LocalDateTime.now().plusDays(7))
            .build();
        given(userRepository.findByRefreshTokenPrefix(prefix)).willReturn(List.of(user));
        given(jwtUtil.generateToken(1L)).willReturn("new-access-token");
        given(jwtUtil.generateRefreshToken()).willReturn("newprefix-new-refresh-token-value");
        given(jwtUtil.getExpirationMs()).willReturn(900000L);
        given(jwtUtil.getRefreshExpirationMs()).willReturn(604800000L);

        AuthResponse res = authService.refresh(rawToken);

        assertThat(res.accessToken()).isEqualTo("new-access-token");
        assertThat(res.refreshToken()).isNotEqualTo(rawToken);
    }

    // WB分岐網羅: prefix未発見 → InvalidTokenException
    @Test
    void refresh_tokenPrefixNotFound_throwsInvalid() {
        String rawToken = "99999999-abcd-efgh-ijkl-mnopqrstuvwx";
        given(userRepository.findByRefreshTokenPrefix("99999999")).willReturn(List.of());

        assertThatThrownBy(() -> authService.refresh(rawToken))
            .isInstanceOf(InvalidTokenException.class);
    }

    // WB条件網羅: prefixはあるがhash不一致 → InvalidTokenException
    @Test
    void refresh_hashMismatch_throwsInvalid() {
        String rawToken = "12345678-abcd-efgh-ijkl-mnopqrstuvwx";
        String prefix = rawToken.substring(0, 8);

        User user = User.builder().id(1L).email("u@example.com").username("u")
            .passwordHash("hashed")
            .refreshTokenPrefix(prefix)
            .refreshTokenHash("wrong-hash-value")  // hash が一致しない
            .refreshTokenExpiresAt(LocalDateTime.now().plusDays(7))
            .build();
        given(userRepository.findByRefreshTokenPrefix(prefix)).willReturn(List.of(user));

        assertThatThrownBy(() -> authService.refresh(rawToken))
            .isInstanceOf(InvalidTokenException.class);
    }

    // WB分岐網羅: 期限切れ → InvalidTokenException
    @Test
    void refresh_tokenExpired_throwsInvalid() {
        String rawToken = "12345678-abcd-efgh-ijkl-mnopqrstuvwx";
        String prefix = rawToken.substring(0, 8);
        String hash = sha256(rawToken);

        User user = User.builder().id(1L).email("u@example.com").username("u")
            .passwordHash("hashed")
            .refreshTokenPrefix(prefix)
            .refreshTokenHash(hash)
            .refreshTokenExpiresAt(LocalDateTime.now().minusDays(1))  // 過去の日時
            .build();
        given(userRepository.findByRefreshTokenPrefix(prefix)).willReturn(List.of(user));

        assertThatThrownBy(() -> authService.refresh(rawToken))
            .isInstanceOf(InvalidTokenException.class);
    }

    // WB条件網羅: expiresAt が null → InvalidTokenException（null側の条件分岐）
    @Test
    void refresh_expiresAtNull_throwsInvalid() {
        String rawToken = "12345678-abcd-efgh-ijkl-mnopqrstuvwx";
        String prefix = rawToken.substring(0, 8);
        String hash = sha256(rawToken);

        User user = User.builder().id(1L).email("u@example.com").username("u")
            .passwordHash("hashed")
            .refreshTokenPrefix(prefix)
            .refreshTokenHash(hash)
            .refreshTokenExpiresAt(null)  // null → isBefore 側は評価されない
            .build();
        given(userRepository.findByRefreshTokenPrefix(prefix)).willReturn(List.of(user));

        assertThatThrownBy(() -> authService.refresh(rawToken))
            .isInstanceOf(InvalidTokenException.class);
    }

    // ─── logout ───────────────────────────────────────────────────────────

    // WB分岐網羅: hash一致 → refresh token フィールドがクリアされ save 呼び出し
    @Test
    void logout_validToken_clearsRefreshToken() {
        String rawToken = "12345678-abcd-efgh-ijkl-mnopqrstuvwx";
        String prefix = rawToken.substring(0, 8);
        String hash = sha256(rawToken);

        User user = User.builder().id(1L).email("u@example.com").username("u")
            .passwordHash("hashed")
            .refreshTokenPrefix(prefix)
            .refreshTokenHash(hash)
            .build();
        given(userRepository.findByRefreshTokenPrefix(prefix)).willReturn(List.of(user));

        authService.logout(rawToken);

        verify(userRepository).save(argThat(u ->
            u.getRefreshTokenPrefix() == null
            && u.getRefreshTokenHash() == null
            && u.getRefreshTokenExpiresAt() == null
        ));
    }

    // WB分岐網羅: token 未発見 → 例外なし・save 未呼び出し
    @Test
    void logout_tokenNotFound_doesNothing() {
        String rawToken = "99999999-abcd-efgh-ijkl-mnopqrstuvwx";
        given(userRepository.findByRefreshTokenPrefix("99999999")).willReturn(List.of());

        authService.logout(rawToken);

        verify(userRepository, never()).save(any());
    }

    // ─── ヘルパー ──────────────────────────────────────────────────────────

    private static String sha256(String input) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
