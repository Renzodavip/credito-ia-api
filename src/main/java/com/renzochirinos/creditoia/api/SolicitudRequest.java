package com.renzochirinos.creditoia.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudRequest(@NotBlank @Size(max = 4000) String texto) {
}
