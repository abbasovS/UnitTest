package com.example.tradems.controller;

import com.example.tradems.dto.request.CreateUserRequest;
import com.example.tradems.dto.request.UpdateBalanceRequest;
import com.example.tradems.model.UserEntity;
import com.example.tradems.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


        @PostMapping("/create")
        public ResponseEntity<UserEntity> createUser(@RequestBody CreateUserRequest request) {
            UserEntity user = userService.createUser(request.username(), request.isPremium());
            return ResponseEntity.ok(user);
        }

        @GetMapping("/{id}")
        public ResponseEntity<UserEntity> getUserById(@PathVariable Long id) {
            UserEntity user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        }

        @PostMapping("/{id}/balance")
        public ResponseEntity<Void> updateBalance(
                @PathVariable Long id,
                @RequestBody UpdateBalanceRequest request) {
            userService.updateUserBalance(id, request.amount());
            return ResponseEntity.ok().build();
        }

        @PostMapping("/reset-balances")
        public ResponseEntity<Void> resetBalances() {
            userService.resetBalances();
            return ResponseEntity.ok().build();
        }

        @GetMapping("/leaderboard")
        public ResponseEntity<List<UserEntity>> getLeaderboard() {
            List<UserEntity> leaderboard = userService.getLeaderboard();
            return ResponseEntity.ok(leaderboard);
        }
    }




