package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.WorkerType;
import java.math.BigDecimal;

public record ServiceCatalogItemResponse(
        WorkerType workerType,
        String title,
        String description,
        BigDecimal startingPrice,
        String etaLabel
) {
}
