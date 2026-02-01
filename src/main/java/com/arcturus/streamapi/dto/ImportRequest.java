package com.arcturus.streamapi.dto;

/**
 * DTO responsável por receber os dados de mídias externas (ex: Jamendo)
 * para serem persistidos na base de dados do Arcturus.
 */
public record ImportRequest(
        String description,
        String s3Url,
        String energyType,
        int frequencyHz
) {}