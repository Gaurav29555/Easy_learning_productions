package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.DispatchConfigurationResponse;
import com.fixcart.fixcart.dto.UpdateDispatchConfigurationRequest;
import com.fixcart.fixcart.entity.DispatchConfiguration;
import com.fixcart.fixcart.repository.DispatchConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DispatchConfigurationService {

    private static final long CONFIG_ID = 1L;

    private final DispatchConfigurationRepository repository;

    @Value("${fixcart.dispatch.stalled-minutes:8}")
    private long defaultStalledMinutesThreshold;

    @Value("${fixcart.dispatch.regression-km:0.75}")
    private double defaultRegressionDistanceKm;

    @Value("${fixcart.dispatch.eta-regression-minutes:8}")
    private long defaultEtaRegressionMinutes;

    @Value("${fixcart.dispatch.inactive-speed-threshold-kmh:3.0}")
    private double defaultInactiveSpeedThresholdKmh;

    @Transactional(readOnly = true)
    public DispatchConfigurationResponse getConfig() {
        return map(getOrCreate());
    }

    @Transactional
    public DispatchConfigurationResponse update(UpdateDispatchConfigurationRequest request) {
        DispatchConfiguration configuration = getOrCreate();
        configuration.setStalledMinutesThreshold(request.stalledMinutesThreshold());
        configuration.setRegressionDistanceKm(request.regressionDistanceKm());
        configuration.setEtaRegressionMinutes(request.etaRegressionMinutes());
        configuration.setInactiveSpeedThresholdKmh(request.inactiveSpeedThresholdKmh());
        return map(repository.save(configuration));
    }

    @Transactional(readOnly = true)
    public DispatchConfiguration getCurrentConfig() {
        return getOrCreate();
    }

    private DispatchConfiguration getOrCreate() {
        return repository.findById(CONFIG_ID).orElseGet(() -> {
            DispatchConfiguration config = new DispatchConfiguration();
            config.setId(CONFIG_ID);
            config.setStalledMinutesThreshold(defaultStalledMinutesThreshold);
            config.setRegressionDistanceKm(defaultRegressionDistanceKm);
            config.setEtaRegressionMinutes(defaultEtaRegressionMinutes);
            config.setInactiveSpeedThresholdKmh(defaultInactiveSpeedThresholdKmh);
            return repository.save(config);
        });
    }

    private DispatchConfigurationResponse map(DispatchConfiguration config) {
        return new DispatchConfigurationResponse(
                config.getStalledMinutesThreshold(),
                config.getRegressionDistanceKm(),
                config.getEtaRegressionMinutes(),
                config.getInactiveSpeedThresholdKmh()
        );
    }
}
