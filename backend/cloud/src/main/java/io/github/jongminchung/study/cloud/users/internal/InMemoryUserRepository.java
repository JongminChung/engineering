package io.github.jongminchung.study.cloud.users.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import io.github.jongminchung.study.cloud.users.domain.User;
import io.github.jongminchung.study.cloud.users.domain.UserRepository;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> store = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        String id = user.id();
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        User saved = new User(id, user.email(), user.displayName());
        store.put(saved.id(), saved);
        return saved;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }
}
