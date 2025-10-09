package com.cinetaste.recipeservice.dto.ai;

import lombok.Data;
import java.util.List;

@Data
public class PairingSuggestionsDto {
    private List<String> drinks;
    private List<String> sideDishes;
}