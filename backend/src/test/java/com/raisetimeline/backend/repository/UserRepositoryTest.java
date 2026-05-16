package com.raisetimeline.backend.repository;

import com.raisetimeline.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired UserRepository userRepository;

    private User alice;
    private User aliceDev;
    private User bob;

    @BeforeEach
    void setUp() {
        // alice はリフレッシュトークン prefix 設定済み
        alice = em.persistAndFlush(User.builder()
            .email("alice@test.com").username("Alice").passwordHash("h")
            .refreshTokenPrefix("abcd1234").refreshTokenHash("somehash").build());
        aliceDev = em.persistAndFlush(User.builder()
            .email("alice_dev@test.com").username("alice_dev").passwordHash("h").build());
        bob = em.persistAndFlush(User.builder()
            .email("bob@test.com").username("Bob").passwordHash("h").build());

        em.clear();
    }

    // BB同値分割（有効）: 存在するメール → Optional 中身あり
    @Test
    void findByEmail_found_returnsUser() {
        Optional<User> result = userRepository.findByEmail("alice@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("Alice");
    }

    // BB同値分割（無効）: 未登録メール → empty
    @Test
    void findByEmail_notFound_returnsEmpty() {
        Optional<User> result = userRepository.findByEmail("unknown@test.com");

        assertThat(result).isEmpty();
    }

    // BB同値分割（有効）: 既存メール → true
    @Test
    void existsByEmail_exists_true() {
        assertThat(userRepository.existsByEmail("bob@test.com")).isTrue();
    }

    // BB同値分割（無効）: 未登録メール → false
    @Test
    void existsByEmail_notExists_false() {
        assertThat(userRepository.existsByEmail("nobody@test.com")).isFalse();
    }

    // BB同値分割（大文字）: "ALICE" → 2件（Alice・alice_dev）
    @Test
    void findByUsernameContainingIgnoreCase_upperCase_matches() {
        List<User> result = userRepository.findByUsernameContainingIgnoreCase("ALICE");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getUsername)
            .containsExactlyInAnyOrder("Alice", "alice_dev");
    }

    // BB同値分割（小文字）: "alice" → 2件
    @Test
    void findByUsernameContainingIgnoreCase_lowerCase_matches() {
        List<User> result = userRepository.findByUsernameContainingIgnoreCase("alice");

        assertThat(result).hasSize(2);
    }

    // BB同値分割（無効）: マッチなし → 空
    @Test
    void findByUsernameContainingIgnoreCase_noMatch_returnsEmpty() {
        List<User> result = userRepository.findByUsernameContainingIgnoreCase("xyz");

        assertThat(result).isEmpty();
    }

    // WB（クエリ確認）: 設定済み prefix → リスト中に対象ユーザー
    @Test
    void findByRefreshTokenPrefix_found_returnsUser() {
        List<User> result = userRepository.findByRefreshTokenPrefix("abcd1234");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("alice@test.com");
    }

    // BB同値分割（無効）: 未設定 prefix → 空
    @Test
    void findByRefreshTokenPrefix_notFound_returnsEmpty() {
        List<User> result = userRepository.findByRefreshTokenPrefix("xxxxxxxx");

        assertThat(result).isEmpty();
    }
}
