package io.github.jongminchung.study.infra.postgresql.phase1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import io.github.jongminchung.study.infra.postgresql.BaseIntegrationTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testContainerIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void testSaveAndFindUser() {
        // Given
        User user = new User("testuser", "test@example.com");

        // When
        User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    void testFindByUsername() {
        // Given
        User user = new User("john", "john@example.com");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByUsername("john");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void testExistsByEmail() {
        // Given
        User user = new User("jane", "jane@example.com");
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByEmail("jane@example.com");
        boolean notExists = userRepository.existsByEmail("notfound@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void testUpdateUser() {
        // Given
        User user = new User("bob", "bob@example.com");
        User savedUser = userRepository.save(user);

        // When
        savedUser.setEmail("newemail@example.com");
        User updatedUser = userRepository.save(savedUser);

        // Then
        assertThat(updatedUser.getEmail()).isEqualTo("newemail@example.com");
        assertThat(updatedUser.getUpdatedAt()).isAfter(updatedUser.getCreatedAt());
    }

    @Test
    void testDeleteUser() {
        // Given
        User user = new User("alice", "alice@example.com");
        User savedUser = userRepository.save(user);

        // When
        userRepository.deleteById(savedUser.getId());

        // Then
        Optional<User> found = userRepository.findById(savedUser.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void testFindAll() {
        // Given
        userRepository.save(new User("user1", "user1@example.com"));
        userRepository.save(new User("user2", "user2@example.com"));
        userRepository.save(new User("user3", "user3@example.com"));

        // When
        var users = userRepository.findAll();

        // Then
        assertThat(users).hasSize(3);
    }
}
