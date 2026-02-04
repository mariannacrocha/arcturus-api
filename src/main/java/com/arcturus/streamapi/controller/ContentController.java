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
public class ContentController {

    private final S3Service s3Service;
    private final ContentRepository contentRepository;
    private final ExternalMediaService externalMediaService;

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
        content.setUser(user);

        return ResponseEntity.ok(contentRepository.save(content));
    }

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
        content.setUser(user);

        VibrationalContent savedContent = contentRepository.save(content);
        return ResponseEntity.ok(savedContent);
    }

    @GetMapping
    public List<VibrationalContent> getAllContents(@AuthenticationPrincipal User user) {
        return contentRepository.findByUser(user);
    }

    @GetMapping("/search")
    public List<VibrationalContent> search(
            @RequestParam("q") String query,
            @AuthenticationPrincipal User user) {

        System.out.println("🔍 Buscando por: " + query);

        List<VibrationalContent> myLibrary = contentRepository.findByUser(user);

        List<VibrationalContent> internalResults = myLibrary.stream()
                .filter(c -> c.getDescription().toLowerCase().contains(query.toLowerCase())
                        || (c.getEnergyType() != null && c.getEnergyType().toLowerCase().contains(query.toLowerCase())))
                .toList();

        List<VibrationalContent> externalResults = externalMediaService.searchFreeMusic(query);

        Set<String> mySavedUrls = myLibrary.stream()
                .map(VibrationalContent::getS3Url)
                .collect(Collectors.toSet());

        List<VibrationalContent> filteredExternal = externalResults.stream()
                .filter(music -> !mySavedUrls.contains(music.getS3Url()))
                .toList();

        return Stream.concat(internalResults.stream(), filteredExternal.stream())
                .collect(Collectors.toList());
    }

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