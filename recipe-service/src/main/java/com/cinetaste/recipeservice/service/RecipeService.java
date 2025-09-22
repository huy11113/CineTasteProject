package com.cinetaste.recipeservice.service;

import com.cinetaste.recipeservice.dto.CreateRecipeRequest;
import com.cinetaste.recipeservice.entity.Recipe;
import com.cinetaste.recipeservice.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public Recipe createRecipe(CreateRecipeRequest request, UUID authorId) {
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle(request.getTitle());
        newRecipe.setSummary(request.getSummary());
        // Đây là nơi bạn sẽ thêm logic để lưu instructions vào bảng recipe_steps sau này
        // newRecipe.setInstructions(request.getInstructions());
        newRecipe.setDifficulty(request.getDifficulty());
        newRecipe.setPrepTimeMinutes(request.getPrepTimeMinutes());
        newRecipe.setCookTimeMinutes(request.getCookTimeMinutes());
        newRecipe.setServings(request.getServings());
        newRecipe.setMainImageUrl(request.getMainImageUrl());
        // newRecipe.setMovieName(request.getMovieName());

        // Gán ID của tác giả
        newRecipe.setAuthorId(authorId);

        // Tạo một slug duy nhất từ tiêu đề
        newRecipe.setSlug(generateSlug(request.getTitle()));

        return recipeRepository.save(newRecipe);
    }

    // Hàm tiện ích để tạo slug (ví dụ: "Món Ăn Ngon" -> "mon-an-ngon")
    private String generateSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH) + "-" + System.currentTimeMillis(); // Thêm timestamp để đảm bảo duy nhất
    }
}