package com.arcturus.streamapi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vibrational_contents")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VibrationalContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "s3url", columnDefinition = "TEXT")
    private String s3Url;

    private LocalDateTime uploadDate;

    private Double frequencyHz;

    @Column(name = "energy_type")
    private String energyType;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "content_type")
    private String contentType; // audio/mpeg, audio/wav

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


}