package com.fixcart.fixcart.repository;

import com.fixcart.fixcart.entity.VoiceConversationMemory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceConversationMemoryRepository extends JpaRepository<VoiceConversationMemory, Long> {

    Optional<VoiceConversationMemory> findByUserId(Long userId);
}
