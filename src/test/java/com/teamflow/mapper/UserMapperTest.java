package com.teamflow.mapper;

import com.teamflow.dto.user.RegisterUserRequestDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toEntityUsesEncodedPasswordAndDefaultRole() {
        RegisterUserRequestDto request =
                new RegisterUserRequestDto("alice", "alice@example.com", "RawPassword123");

        User user = mapper.toEntity(request, "$2a$encoded-password");

        assertEquals("alice", user.getUsername());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("$2a$encoded-password", user.getPassword());
        assertEquals(UserRole.USER, user.getRole());
    }

    @Test
    void toResponseDtoDoesNotExposePassword() {
        User user = new User("alice", "alice@example.com", "$2a$encoded-password", UserRole.USER);

        UserResponseDto response = mapper.toResponseDto(user);

        assertEquals("alice", response.username());
        assertEquals("alice@example.com", response.email());
        assertEquals(UserRole.USER, response.role());
        assertFalse(Arrays.stream(UserResponseDto.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("password")));
        assertNull(response.id());
    }
}
