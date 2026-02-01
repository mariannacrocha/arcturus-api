package com.arcturus.streamapi.controller;

import com.arcturus.streamapi.domain.VibrationalContent;
import com.arcturus.streamapi.dto.ImportRequest;
import com.arcturus.streamapi.repository.ContentRepository;
import com.arcturus.streamapi.service.ExternalMediaService;
import com.arcturus.streamapi.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/v1/contents")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Permite o acesso do seu Angular
public class ContentController {

    private final S3Service s3Service;
    private final ContentRepository contentRepository;
    private final ExternalMediaService externalMediaService; // Injeção do novo serviço

    // Endpoint de Upload (mantido para o seu uso administrativo)
    @PostMapping("/upload")
    public ResponseEntity<VibrationalContent> uploadContent(
            @RequestParam("file") MultipartFile file,
            @RequestParam("description") String description,
            @RequestParam("frequencyHz") int frequencyHz,
            @RequestParam("energyType") String energyType) {

        String s3Url = s3Service.uploadFile(file);

        VibrationalContent content = new VibrationalContent();
        content.setDescription(description);
        content.setFrequencyHz((double) frequencyHz);
        content.setEnergyType(energyType);
        content.setS3Url(s3Url);

        return ResponseEntity.ok(contentRepository.save(content));
    }
    @PostMapping("/import")
    public ResponseEntity<VibrationalContent> importContent(@RequestBody ImportRequest request) {

        VibrationalContent content = new VibrationalContent();

        content.setDescription(request.description());

        // 🚀 CORREÇÃO AQUI: Preenchemos o Título copiando a Descrição
        content.setTitle(request.description());

        content.setS3Url(request.s3Url());
        content.setEnergyType(request.energyType());
        content.setFrequencyHz((double) request.frequencyHz());

        // Mantém a correção anterior do UUID
        content.setS3Key("external-" + java.util.UUID.randomUUID().toString());

        // Dica Extra: Se o banco reclamar de 'upload_date' depois, descomente a linha abaixo:
        // content.setUploadDate(java.time.LocalDateTime.now());

        VibrationalContent savedContent = contentRepository.save(content);

        return ResponseEntity.ok(savedContent);
    }

    // Endpoint de Listagem Geral (carrega tudo ao abrir a página)
    @GetMapping
    public List<VibrationalContent> getAllContents() {
        return contentRepository.findAll();
    }

    // NOVO: Endpoint de Busca Híbrida
    @GetMapping("/search")
    public List<VibrationalContent> search(@RequestParam("q") String query) {
        System.out.println("🔍 Buscando por: " + query);


        List<VibrationalContent> internalResults = contentRepository
                .findByDescriptionContainingIgnoreCaseOrEnergyTypeContainingIgnoreCase(query, query);

        System.out.println("✅ Encontrados no Banco Local: " + internalResults.size());

        // 2. Busca Externa (Jamendo)
        List<VibrationalContent> externalResults = externalMediaService.searchFreeMusic(query);
        System.out.println("✅ Encontrados no Jamendo: " + externalResults.size());

        // 3. Une as listas
        return Stream.concat(internalResults.stream(), externalResults.stream())
                .collect(Collectors.toList());
    }
}