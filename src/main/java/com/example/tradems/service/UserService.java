package com.example.tradems.service;
import com.example.tradems.enums.UserRank;
import com.example.tradems.exception.UserNotFoundException;
import com.example.tradems.model.UserEntity;
import com.example.tradems.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;


    public UserEntity createUser(String username, boolean isPremium) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPremium(isPremium);
        if (isPremium) {
            user.setVirtualBalance(BigDecimal.valueOf(10_000));
            user.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));
        } else {
            user.setVirtualBalance(BigDecimal.ZERO);
        }
        user.setUserRank(UserRank.ROOKIE);
        return userRepository.save(user);
    }


    public UserEntity getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(()-> new UserNotFoundException("User not found"));

    }

    @Transactional
    public void updateUserBalance(Long userId, BigDecimal amount) {
        UserEntity user = getUserById(userId);
        if (!user.isPremium()) {
            return;
        }
        user.setVirtualBalance(user.getVirtualBalance().add(amount));
        userRepository.save(user);
    }

    public void resetBalances() {
        List<UserEntity> users = userRepository.findAll();
        for (UserEntity user : users) {
            if (user.isPremium()) {
                user.setVirtualBalance(BigDecimal.valueOf(10_000));
            }
        }
        userRepository.saveAll(users);
    }

    public List<UserEntity> getLeaderboard() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "virtualBalance"));
    }
}
