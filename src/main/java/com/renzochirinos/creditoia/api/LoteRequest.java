package com.renzochirinos.creditoia.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LoteRequest(
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 4000) String> solicitudes) {
}
