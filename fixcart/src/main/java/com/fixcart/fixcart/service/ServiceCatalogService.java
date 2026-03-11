package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.ServiceCatalogItemResponse;
import com.fixcart.fixcart.entity.enums.WorkerType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServiceCatalogService {

    public List<ServiceCatalogItemResponse> getCatalog() {
        return List.of(
                new ServiceCatalogItemResponse(WorkerType.PLUMBER, "Plumber", "Leaks, taps, pipelines and bathroom fittings.", BigDecimal.valueOf(249), "15-25 min"),
                new ServiceCatalogItemResponse(WorkerType.CARPENTER, "Carpenter", "Door repair, furniture fixes and woodwork jobs.", BigDecimal.valueOf(299), "20-35 min"),
                new ServiceCatalogItemResponse(WorkerType.ELECTRICIAN, "Electrician", "Switchboards, wiring faults and fixture installation.", BigDecimal.valueOf(279), "15-25 min"),
                new ServiceCatalogItemResponse(WorkerType.CLEANER, "Cleaner", "Home deep cleaning and urgent spill cleanup.", BigDecimal.valueOf(399), "30-45 min"),
                new ServiceCatalogItemResponse(WorkerType.AC_REPAIR, "AC Repair", "Cooling issue diagnosis, servicing and urgent repair.", BigDecimal.valueOf(449), "25-40 min"),
                new ServiceCatalogItemResponse(WorkerType.APPLIANCE_REPAIR, "Appliance Repair", "Washing machine, fridge and kitchen appliance support.", BigDecimal.valueOf(449), "25-40 min"),
                new ServiceCatalogItemResponse(WorkerType.PAINTER, "Painter", "Patch paint, wall touch-up and repaint support.", BigDecimal.valueOf(599), "45-60 min")
        );
    }
}
