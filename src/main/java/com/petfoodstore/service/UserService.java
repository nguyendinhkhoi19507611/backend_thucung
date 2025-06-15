package com.petfoodstore.service;

import com.petfoodstore.entity.User;
import com.petfoodstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(User user) {
        User savedUser = userRepository.save(user);

        // Send welcome notification for new customers
        if (user.getRole() == User.UserRole.CUSTOMER) {
            notificationService.createWelcomeNotification(savedUser);
        }

        return savedUser;
    }

    public User updateUser(Long id, User userDetails) {
        User user = findById(id);
        user.setFullName(userDetails.getFullName());
        user.setPhone(userDetails.getPhone());
        user.setAddress(userDetails.getAddress());
        if (userDetails.getEmail() != null) {
            user.setEmail(userDetails.getEmail());
        }
        if (userDetails.getPassword() != null) {
            user.setPassword(userDetails.getPassword());
        }
        if (userDetails.getRole() != null) {
            user.setRole(userDetails.getRole());
        }
        return userRepository.save(user);
    }

    public void toggleUserStatus(Long id) {
        User user = findById(id);
        user.setActive(!user.getActive());
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = findById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}