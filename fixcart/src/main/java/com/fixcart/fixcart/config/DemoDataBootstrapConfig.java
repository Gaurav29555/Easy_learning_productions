package com.fixcart.fixcart.config;

import com.fixcart.fixcart.entity.User;
import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.entity.enums.WorkerApprovalStatus;
import com.fixcart.fixcart.entity.enums.WorkerType;
import com.fixcart.fixcart.repository.UserRepository;
import com.fixcart.fixcart.repository.WorkerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DemoDataBootstrapConfig {

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${fixcart.demo.bootstrap.enabled:true}")
    private boolean demoBootstrapEnabled;

    @Value("${fixcart.demo.bootstrap.password:demo123}")
    private String demoPassword;

    @Bean
    @Order(1)
    public CommandLineRunner fixcartDemoWorkerBootstrapRunner() {
        return args -> {
            if (!demoBootstrapEnabled) {
                return;
            }

            List<DemoWorkerSeed> seeds = List.of(
                    new DemoWorkerSeed("Aarav Plumber", "demo.plumber.baner@fixcart.com", "9100000001", WorkerType.PLUMBER, 18.5679, 73.7671, 6),
                    new DemoWorkerSeed("Nikhil Carpenter", "demo.carpenter.baner@fixcart.com", "9100000002", WorkerType.CARPENTER, 18.5590, 73.7868, 5),
                    new DemoWorkerSeed("Priya Electrician", "demo.electrician.pune@fixcart.com", "9100000003", WorkerType.ELECTRICIAN, 18.5204, 73.8567, 7),
                    new DemoWorkerSeed("Meera Cleaner", "demo.cleaner.andheri@fixcart.com", "9100000004", WorkerType.CLEANER, 19.1197, 72.8468, 4),
                    new DemoWorkerSeed("Rohan AC Repair", "demo.ac.hsr@fixcart.com", "9100000005", WorkerType.AC_REPAIR, 12.9121, 77.6446, 8),
                    new DemoWorkerSeed("Kabir Appliance Repair", "demo.appliance.cp@fixcart.com", "9100000006", WorkerType.APPLIANCE_REPAIR, 28.6315, 77.2167, 6),
                    new DemoWorkerSeed("Sana Painter", "demo.painter.powai@fixcart.com", "9100000007", WorkerType.PAINTER, 19.1176, 72.9060, 9)
            );

            for (DemoWorkerSeed seed : seeds) {
                if (userRepository.existsByEmail(seed.email())) {
                    continue;
                }

                User user = new User();
                user.setFullName(seed.fullName());
                user.setEmail(seed.email().toLowerCase());
                user.setPassword(passwordEncoder.encode(demoPassword));
                user.setPhone(seed.phone());
                user.setRole(UserRole.WORKER);
                User savedUser = userRepository.save(user);

                Worker worker = new Worker();
                worker.setUser(savedUser);
                worker.setWorkerType(seed.workerType());
                worker.setApprovalStatus(WorkerApprovalStatus.APPROVED);
                worker.setYearsOfExperience(seed.yearsOfExperience());
                worker.setLatitude(seed.latitude());
                worker.setLongitude(seed.longitude());
                worker.setAvailable(true);
                workerRepository.save(worker);
            }

            log.info("fixcart demo worker bootstrap finished");
        };
    }

    private record DemoWorkerSeed(
            String fullName,
            String email,
            String phone,
            WorkerType workerType,
            double latitude,
            double longitude,
            int yearsOfExperience
    ) {
    }
}
