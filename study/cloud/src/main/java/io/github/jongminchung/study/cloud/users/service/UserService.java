package io.github.jongminchung.study.cloud.users.service;

import org.springframework.stereotype.Service;

import io.github.jongminchung.study.cloud.users.domain.User;
import io.github.jongminchung.study.cloud.users.domain.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(String email, String displayName) {
        return userRepository.save(new User(null, email, displayName));
    }

    public User find(String id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }
}
