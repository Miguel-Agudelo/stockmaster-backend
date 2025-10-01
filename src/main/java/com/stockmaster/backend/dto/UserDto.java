package com.stockmaster.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserDto {
    private String name;
    private String email;
    private String password;
    private String role;
    private LocalDateTime createdAt;
}
