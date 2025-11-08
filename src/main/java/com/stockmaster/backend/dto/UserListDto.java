package com.stockmaster.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserListDto {
    private Long id;
    private String name;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}