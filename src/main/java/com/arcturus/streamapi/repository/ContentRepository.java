package com.arcturus.streamapi.repository;

import com.arcturus.streamapi.domain.User;
import com.arcturus.streamapi.domain.VibrationalContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<VibrationalContent, UUID> {

    // Busca músicas pelo texto (Já existia)
    List<VibrationalContent> findByDescriptionContainingIgnoreCaseOrEnergyTypeContainingIgnoreCase(String description, String energyType);

    // 🚀 NOVO: Busca uma música específica, mas só se pertencer ao dono
    Optional<VibrationalContent> findByIdAndUser(UUID id, User user);

    List<VibrationalContent> findByUser(User user);
}