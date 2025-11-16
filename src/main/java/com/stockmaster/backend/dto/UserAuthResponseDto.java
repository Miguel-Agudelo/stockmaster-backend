package com.stockmaster.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAuthResponseDto {
    private Long idUser;
    private String name;
    private String role;
}