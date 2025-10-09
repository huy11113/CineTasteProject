package com.cinetaste.userservice.dto;

import lombok.Data;

import java.util.Map;

@Data
public class FlavorProfileRequest {
    private Map<String, Object> preferences;
}