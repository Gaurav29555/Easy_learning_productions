package com.fixcart.fixcart.service;

import com.fixcart.fixcart.entity.VoiceConversationMemory;
import com.fixcart.fixcart.entity.enums.WorkerType;
import com.fixcart.fixcart.repository.VoiceConversationMemoryRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoiceConversationMemoryService {

    private final VoiceConversationMemoryRepository memoryRepository;

    @Value("${fixcart.voice.memory-minutes:20}")
    private long memoryMinutes;

    public VoiceConversationMemory getActiveMemory(Long userId) {
        return memoryRepository.findByUserId(userId)
                .filter(memory -> memory.getExpiresAt() != null && memory.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    @Transactional
    public void remember(Long userId, String action, WorkerType workerType, String addressHint, Double latitude, Double longitude, Long bookingId) {
        VoiceConversationMemory memory = memoryRepository.findByUserId(userId).orElseGet(VoiceConversationMemory::new);
        memory.setUserId(userId);
        memory.setLastAction(action);
        memory.setLastWorkerType(workerType);
        memory.setLastAddressHint(addressHint);
        memory.setLastLatitude(latitude);
        memory.setLastLongitude(longitude);
        memory.setLastBookingId(bookingId);
        memory.setExpiresAt(LocalDateTime.now().plusMinutes(memoryMinutes));
        memoryRepository.save(memory);
    }
}
