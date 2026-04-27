package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.ProductChangeLogDto;
import com.stockmaster.backend.entity.Product;
import com.stockmaster.backend.entity.ProductChangeLog;
import com.stockmaster.backend.entity.User;
import com.stockmaster.backend.repository.ProductChangeLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductChangeLogService {

    @Autowired
    private ProductChangeLogRepository changeLogRepository;

    /**
     * Registra un cambio individual en un campo de un producto.
     * Llamado desde ProductService.updateProduct().
     */
    public void recordChange(Product product, User changedBy,
                             String fieldName, String oldValue, String newValue) {
        // Solo registrar si el valor efectivamente cambió
        if (oldValue == null && newValue == null) return;
        if (oldValue != null && oldValue.equals(newValue)) return;

        ProductChangeLog log = new ProductChangeLog();
        log.setProduct(product);
        log.setChangedBy(changedBy);
        log.setFieldName(fieldName);
        log.setOldValue(oldValue != null ? oldValue : "—");
        log.setNewValue(newValue != null ? newValue : "—");

        changeLogRepository.save(log);
    }

    /**
     * Retorna el historial de cambios de un producto en orden cronológico descendente.
     */
    public List<ProductChangeLogDto> getChangeLogByProduct(Long productId) {
        return changeLogRepository.findByProductIdOrderByChangedAtDesc(productId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ProductChangeLogDto toDto(ProductChangeLog log) {
        ProductChangeLogDto dto = new ProductChangeLogDto();
        dto.setId(log.getId());
        dto.setProductId(log.getProduct().getId());
        dto.setProductName(log.getProduct().getName());
        dto.setChangedByName(log.getChangedBy() != null ? log.getChangedBy().getName() : "Sistema");
        dto.setFieldName(log.getFieldName());
        dto.setOldValue(log.getOldValue());
        dto.setNewValue(log.getNewValue());
        dto.setChangedAt(log.getChangedAt());
        return dto;
    }
}