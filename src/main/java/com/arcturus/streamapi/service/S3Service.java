package com.arcturus.streamapi.service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    // Injetando valores do application.properties
    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    public String uploadFile(MultipartFile file) {
        // 1. Gera um nome único para o arquivo (ex: a1b2c3d4-meditacao.mp3)
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

        try {
            // 2. Prepara a requisição de upload
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType()) // Importante para o navegador saber que é áudio
                    .build();

            // 3. Envia o binário para a AWS
            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 4. Retorna a URL pública formatada (já que seu bucket é público para leitura)
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler o arquivo para upload", e);
        }
    }
}