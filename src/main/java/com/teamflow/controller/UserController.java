package com.teamflow.controller;

import com.teamflow.dto.user.UpdateUserProfileRequestDto;
import com.teamflow.dto.user.ChangePasswordRequestDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(userService.getProfile(jwt.getSubject()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDto> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequestDto request
    ) {
        return ResponseEntity.ok(userService.updateProfile(jwt.getSubject(), request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequestDto request
    ) {
        userService.changePassword(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
