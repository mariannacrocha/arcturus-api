package com.arcturus.streamapi.service;

import com.arcturus.streamapi.domain.VibrationalContent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExternalMediaService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${jamendo.client-id}")
    private String clientId;

    @Value("${jamendo.url}")
    private String apiUrl;

    private static final Map<String, String> DOMAIN_DICTIONARY = new HashMap<>();

    static {
        // 🚀 MUDANÇA 1: Chaves SEM acento (Seguro contra erros de Windows/Linux)
        DOMAIN_DICTIONARY.put("meditacao", "meditation");
        DOMAIN_DICTIONARY.put("cura", "healing");
        DOMAIN_DICTIONARY.put("sono", "sleep");
        DOMAIN_DICTIONARY.put("frequencia", "frequency");
        DOMAIN_DICTIONARY.put("natureza", "nature");
        DOMAIN_DICTIONARY.put("relaxar", "relax");
        DOMAIN_DICTIONARY.put("agua", "water");
        DOMAIN_DICTIONARY.put("paz", "peace");
        DOMAIN_DICTIONARY.put("amor", "love");
        DOMAIN_DICTIONARY.put("floresta", "forest");
    }

    public ExternalMediaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // 🚀 MUDANÇA 2: Método para remover acentos (Meditação -> meditacao)
    private String removeAccents(String value) {
        String normalizer = Normalizer.normalize(value, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalizer).replaceAll("");
    }

    public List<VibrationalContent> searchFreeMusic(String query) {
        if (query == null || query.isBlank()) return new ArrayList<>();

        // Tratamento da entrada
        String termRaw = query.trim().toLowerCase();
        String termNormalized = removeAccents(termRaw); // "meditação" vira "meditacao"

        // Busca tradução no dicionário seguro
        String termEn = DOMAIN_DICTIONARY.get(termNormalized);

        System.out.println("🔎 Busca Iniciada | Original: " + termRaw + " | Normalizado: " + termNormalized + " | Tradução: " + termEn);

        // Dispara busca pelo termo original (PT)
        CompletableFuture<List<VibrationalContent>> searchPt = CompletableFuture.supplyAsync(() ->
                fetchFromJamendo(termRaw)
        );

        // Se houver tradução, dispara busca em Inglês em paralelo
        CompletableFuture<List<VibrationalContent>> searchEn = (termEn != null)
                ? CompletableFuture.supplyAsync(() -> fetchFromJamendo(termEn))
                : CompletableFuture.completedFuture(new ArrayList<>());

        // Une os resultados
        return Stream.concat(searchPt.join().stream(), searchEn.join().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<VibrationalContent> fetchFromJamendo(String searchTerm) {
        try {
            String encodedQuery = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);

            // 🚀 CORREÇÃO AQUI: Mudamos de 'popularity' para 'popularity_total'
            String finalUrl = String.format("%s?client_id=%s&format=json&limit=10&boost=popularity_total&search=%s",
                    apiUrl, clientId, encodedQuery);

            System.out.println("🔗 URL Corrigida: " + finalUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseJamendoResponse(response.body());
            } else {
                System.err.println(" Erro Jamendo (" + response.statusCode() + "): " + response.body());
            }

        } catch (Exception e) {
            System.err.println(" Exceção Java: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private List<VibrationalContent> parseJamendoResponse(String jsonBody) {
        List<VibrationalContent> resultList = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode results = root.path("results");

            if (results.isArray()) {
                for (JsonNode node : results) {
                    VibrationalContent content = new VibrationalContent();

                    String title = node.path("name").asText("Sem Título");
                    String artist = node.path("artist_name").asText("Artista Desconhecido");

                    content.setDescription(title + " - " + artist);
                    content.setS3Url(node.path("audio").asText());
                    content.setEnergyType("JAMENDO_REAL");
                    content.setFrequencyHz(432.0);

                    if (!content.getS3Url().isEmpty()) {
                        resultList.add(content);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro parser: " + e.getMessage());
        }
        return resultList;
    }
}