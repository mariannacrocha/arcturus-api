package com.arcturus.streamapi.repository;

import com.arcturus.streamapi.domain.VibrationalContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<VibrationalContent, Long> {

    // 🚀 MELHORIA: Busca "OU" (Or).
    // Procura o termo na 'description' OU no 'energyType'
    // O 'ContainingIgnoreCase' garante que "medit" encontre "Meditation" (busca parcial)
    List<VibrationalContent> findByDescriptionContainingIgnoreCaseOrEnergyTypeContainingIgnoreCase(String description, String energyType);
}