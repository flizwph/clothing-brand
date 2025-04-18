package com.brand.backend.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@AllArgsConstructor
public class NFTDto {
    private Long id;
    private String placeholderUri;
    private String revealedUri;
    private boolean revealed;
    private String rarity;
    private LocalDateTime createdAt;
}