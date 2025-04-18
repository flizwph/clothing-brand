package com.brand.backend.presentation.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class RevealRequest {
    private String revealedUri;
}