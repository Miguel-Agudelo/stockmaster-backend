package com.stockmaster.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class UserDto {
    private String name;
    private String email;
    private String password;
    private String role;
}
