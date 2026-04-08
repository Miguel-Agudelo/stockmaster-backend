package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.SupplierDto;
import com.stockmaster.backend.dto.SupplierListDto;
import com.stockmaster.backend.entity.Supplier;
import com.stockmaster.backend.repository.SupplierRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    public List<SupplierListDto> getAllActiveSuppliers() {
        return supplierRepository.findByIsActiveTrue().stream()
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    public List<SupplierListDto> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    // Papelera: solo inactivos
    public List<SupplierListDto> getAllInactiveSuppliers() {
        return supplierRepository.findAll().stream()
                .filter(s -> !s.isActive())
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupplierListDto createSupplier(SupplierDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio.");
        }
        if (supplierRepository.findByName(dto.getName().trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un proveedor con el nombre: " + dto.getName());
        }
        if (dto.getNit() != null && !dto.getNit().isBlank()) {
            if (supplierRepository.findByNit(dto.getNit().trim()).isPresent()) {
                throw new IllegalArgumentException("Ya existe un proveedor con el NIT: " + dto.getNit());
            }
        }

        Supplier supplier = new Supplier();
        supplier.setName(dto.getName().trim());
        supplier.setNit(dto.getNit() != null ? dto.getNit().trim() : null);
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        supplier.setActive(true);

        return toListDto(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierListDto updateSupplier(Long id, SupplierDto dto) {
        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado con ID: " + id));

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio.");
        }
        supplierRepository.findByName(dto.getName().trim()).ifPresent(found -> {
            if (!found.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe otro proveedor con el nombre: " + dto.getName());
            }
        });
        if (dto.getNit() != null && !dto.getNit().isBlank()) {
            supplierRepository.findByNit(dto.getNit().trim()).ifPresent(found -> {
                if (!found.getId().equals(id)) {
                    throw new IllegalArgumentException("Ya existe otro proveedor con el NIT: " + dto.getNit());
                }
            });
        }

        existing.setName(dto.getName().trim());
        existing.setNit(dto.getNit() != null ? dto.getNit().trim() : null);
        existing.setPhone(dto.getPhone());
        existing.setEmail(dto.getEmail());
        existing.setAddress(dto.getAddress());

        return toListDto(supplierRepository.save(existing));
    }

    @Transactional
    public void deactivateSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado con ID: " + id));
        if (!supplier.isActive()) {
            throw new IllegalStateException("El proveedor ya se encuentra inactivo.");
        }
        supplier.setActive(false);
        supplier.setDeletedAt(LocalDateTime.now());
        supplierRepository.save(supplier);
    }

    // Papelera: restaurar proveedor inactivo
    @Transactional
    public SupplierListDto restoreSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado con ID: " + id));
        if (supplier.isActive()) {
            throw new IllegalStateException("El proveedor ya se encuentra activo.");
        }
        supplier.setActive(true);
        supplier.setDeletedAt(null);
        return toListDto(supplierRepository.save(supplier));
    }

    private SupplierListDto toListDto(Supplier s) {
        SupplierListDto dto = new SupplierListDto();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setNit(s.getNit());
        dto.setPhone(s.getPhone());
        dto.setEmail(s.getEmail());
        dto.setAddress(s.getAddress());
        dto.setActive(s.isActive());
        dto.setProductCount(supplierRepository.countActiveProductsBySupplierId(s.getId()));
        return dto;
    }
}
