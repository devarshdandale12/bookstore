package com.devarsh.bookstore.dto;

import com.devarsh.bookstore.entity.Role;
import com.devarsh.bookstore.entity.User;

public record UserResponse(Long id, String name, String email, Role role) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
