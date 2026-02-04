package com.arcturus.streamapi.controller;

import com.arcturus.streamapi.domain.User;
import com.arcturus.streamapi.domain.VibrationalContent;
import com.arcturus.streamapi.dto.ImportRequest;
import com.arcturus.streamapi.repository.ContentRepository;
import com.arcturus.streamapi.service.ExternalMediaService;
import com.arcturus.streamapi.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/v1/contents")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ContentController {

    private final S3Service s3Service;
    private final ContentRepository contentRepository;
    private final ExternalMediaService externalMediaService;

    // 1. Endpoint de Upload
    @PostMapping("/upload")
    public ResponseEntity<VibrationalContent> uploadContent(
            @RequestParam("file") MultipartFile file,
            @RequestParam("description") String description,
            @RequestParam("frequencyHz") int frequencyHz,
            @RequestParam("energyType") String energyType,
            @AuthenticationPrincipal User user) {

        String s3Url = s3Service.uploadFile(file);

        VibrationalContent content = new VibrationalContent();
        content.setDescription(description);
        content.setTitle(description);
        content.setFrequencyHz((double) frequencyHz);
        content.setEnergyType(energyType);
        content.setS3Url(s3Url);
        content.setUser(user); // Vincula ao usuário

        return ResponseEntity.ok(contentRepository.save(content));
    }

    // 2. Endpoint de Importar (Salvar)
    @PostMapping("/import")
    public ResponseEntity<VibrationalContent> importContent(
            @RequestBody ImportRequest request,
            @AuthenticationPrincipal User user) {

        VibrationalContent content = new VibrationalContent();
        content.setDescription(request.description());
        content.setTitle(request.description());
        content.setS3Url(request.s3Url());
        content.setEnergyType(request.energyType());
        content.setFrequencyHz((double) request.frequencyHz());
        content.setS3Key("external-" + java.util.UUID.randomUUID().toString());
        content.setUser(user); // Vincula ao usuário

        VibrationalContent savedContent = contentRepository.save(content);
        return ResponseEntity.ok(savedContent);
    }

    // 3. Endpoint de Listagem (Minha Biblioteca)
    // 🚀 ALTERADO: Agora recebe o User e busca SÓ o que é dele
    @GetMapping
    public List<VibrationalContent> getAllContents(@AuthenticationPrincipal User user) {
        return contentRepository.findByUser(user);
    }

    // 4. Endpoint de Busca Híbrida
    // 🚀 ALTERADO: Agora filtra os resultados internos baseados no usuário
    @GetMapping("/search")
    public List<VibrationalContent> search(
            @RequestParam("q") String query,
            @AuthenticationPrincipal User user) { // Recebe o usuário para filtrar corretamente

        System.out.println("🔍 Buscando por: " + query);

        // A. Busca Interna (No banco)
        // ⚠️ Truque: Buscamos tudo do usuário primeiro e filtramos na memória
        // (Para evitar criar querys complexas no Repository agora)
        List<VibrationalContent> myLibrary = contentRepository.findByUser(user);

        List<VibrationalContent> internalResults = myLibrary.stream()
                .filter(c -> c.getDescription().toLowerCase().contains(query.toLowerCase())
                        || (c.getEnergyType() != null && c.getEnergyType().toLowerCase().contains(query.toLowerCase())))
                .toList();

        // B. Busca Externa (Jamendo)
        List<VibrationalContent> externalResults = externalMediaService.searchFreeMusic(query);

        // C. Deduplicação (Remove do externo o que EU já tenho salvo)
        Set<String> mySavedUrls = myLibrary.stream()
                .map(VibrationalContent::getS3Url)
                .collect(Collectors.toSet());

        List<VibrationalContent> filteredExternal = externalResults.stream()
                .filter(music -> !mySavedUrls.contains(music.getS3Url()))
                .toList();

        // D. Une as listas
        return Stream.concat(internalResults.stream(), filteredExternal.stream())
                .collect(Collectors.toList());
    }

    // 5. Endpoint de Deletar
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        var content = contentRepository.findByIdAndUser(id, user);

        if (content.isPresent()) {
            contentRepository.delete(content.get());
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}