package vn.hcmute.rebook.dto;

import jakarta.validation.constraints.NotBlank;

public record ApiLoginRequest(@NotBlank String email, @NotBlank String password) {}
