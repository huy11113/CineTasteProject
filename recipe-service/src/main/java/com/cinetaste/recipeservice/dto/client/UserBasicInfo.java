package com.cinetaste.recipeservice.dto.client;

import lombok.Data;
import java.util.UUID;

@Data
public class UserBasicInfo {
    private UUID id;
    private String username;
    private String displayName;
    private String profileImageUrl;
}