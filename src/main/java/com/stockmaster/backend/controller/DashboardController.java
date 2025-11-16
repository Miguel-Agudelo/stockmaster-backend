package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.DashboardMetricDto;
import com.stockmaster.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * Endpoint para obtener todos los datos resumidos del Dashboard.
     * Delega la lógica de negocio al DashboardService.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<DashboardMetricDto> getDashboardSummary() {

        // 1. Obtener el nombre del usuario logueado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "Usuario";

        // 2. Llamar al servicio para obtener todos los datos ya calculados y agrupados
        DashboardMetricDto summary = dashboardService.getDashboardSummary(username);

        // 3. Devolver la respuesta
        return ResponseEntity.ok(summary);
    }
}