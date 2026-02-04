package com.arcturus.streamapi.dto;


public record ImportRequest(
        String description,
        String s3Url,
        String energyType,
        int frequencyHz
) {}