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
import java.util.regex.Matcher;
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

    // Regex para encontrar frequências comuns no meio do texto
    private static final Pattern FREQUENCY_PATTERN = Pattern.compile("\\b(174|285|396|417|432|440|528|639|741|852|963)\\b");

    private static final Map<String, String> DOMAIN_DICTIONARY = new HashMap<>();

    static {
        DOMAIN_DICTIONARY.put("meditacao", "meditation");
        DOMAIN_DICTIONARY.put("cura", "healing");
        DOMAIN_DICTIONARY.put("sono", "sleep");
        DOMAIN_DICTIONARY.put("frequencia", "frequency");
        DOMAIN_DICTIONARY.put("natureza", "nature");
        DOMAIN_DICTIONARY.put("relaxar", "relax");
    }

    public ExternalMediaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private String removeAccents(String value) {
        String normalizer = Normalizer.normalize(value, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalizer).replaceAll("");
    }

    public List<VibrationalContent> searchFreeMusic(String query) {
        if (query == null || query.isBlank()) return new ArrayList<>();

        String termRaw = query.trim().toLowerCase();
        String termNormalized = removeAccents(termRaw);
        String termEn = DOMAIN_DICTIONARY.get(termNormalized);

        System.out.println("🔎 Busca Realista: " + termRaw);

        CompletableFuture<List<VibrationalContent>> searchPt = CompletableFuture.supplyAsync(() ->
                fetchFromJamendo(termRaw)
        );

        CompletableFuture<List<VibrationalContent>> searchEn = (termEn != null)
                ? CompletableFuture.supplyAsync(() -> fetchFromJamendo(termEn))
                : CompletableFuture.completedFuture(new ArrayList<>());

        return Stream.concat(searchPt.join().stream(), searchEn.join().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<VibrationalContent> fetchFromJamendo(String searchTerm) {
        try {
            String encodedQuery = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);

            // Inclui musicinfo para buscar nas tags
            String finalUrl = String.format("%s?client_id=%s&format=json&limit=10&boost=popularity_total&include=musicinfo&search=%s",
                    apiUrl, clientId, encodedQuery);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseJamendoResponse(response.body());
            }

        } catch (Exception e) {
            System.err.println(" Exceção Java: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // 🚀 ATUALIZADO: Retorna NULL se não achar nada
    private Double extractFrequencyFromMetadata(String title, JsonNode tagsNode) {
        StringBuilder textToScan = new StringBuilder(title);

        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tag : tagsNode) {
                textToScan.append(" ").append(tag.asText());
            }
        }

        Matcher matcher = FREQUENCY_PATTERN.matcher(textToScan.toString());

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        // Se não achou, retorna nulo (sem informação)
        return null;
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
                    JsonNode tags = node.path("musicinfo").path("tags");

                    content.setDescription(title + " - " + artist);
                    content.setS3Url(node.path("audio").asText());
                    content.setEnergyType("JAMENDO_REAL");

                    // Pode setar NULL aqui, sem problemas
                    Double realFrequency = extractFrequencyFromMetadata(title, tags);
                    content.setFrequencyHz(realFrequency);

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