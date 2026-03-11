package com.fixcart.fixcart.controller;

import com.fixcart.fixcart.dto.ServiceCatalogItemResponse;
import com.fixcart.fixcart.service.ServiceCatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping("/services")
    public ResponseEntity<List<ServiceCatalogItemResponse>> getServices() {
        return ResponseEntity.ok(serviceCatalogService.getCatalog());
    }
}
