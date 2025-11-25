"""
ai_service/services/create_by_theme.py
Sáng tạo món ăn mới dựa trên chủ đề (phim, thể loại, văn hóa)
"""

import logging
from typing import Dict, Any, List
from datetime import datetime

from pydantic import BaseModel, Field, validator
import google.generativeai as genai

logger = logging.getLogger(__name__)

# ============================================================================
# PYDANTIC MODELS (Reuse from analyze_dish)
# ============================================================================

class MovieContext(BaseModel):
    title: str = Field(..., description="Tên phim/show gợi cảm hứng")
    scene_description: str = Field(..., description="Mô tả cảnh liên quan")
    significance: str = Field(..., description="Ý nghĩa của món ăn trong bối cảnh")
    wikipedia_link: str = Field(default="", description="URL Wikipedia của phim")

class NutritionEstimate(BaseModel):
    calories: int = Field(..., ge=0, le=5000)
    protein_g: int = Field(..., ge=0, le=500)
    carbs_g: int = Field(..., ge=0, le=1000)
    fat_g: int = Field(..., ge=0, le=500)

class PairingSuggestions(BaseModel):
    drinks: List[str] = Field(default_factory=list, max_items=5)
    side_dishes: List[str] = Field(default_factory=list, max_items=5)

class RecipeIngredient(BaseModel):
    name: str
    quantity: str
    unit: str = ""

class RecipeInstruction(BaseModel):
    step: int = Field(..., ge=1, le=50)
    description: str

class RecipeDetail(BaseModel):
    difficulty: int = Field(..., ge=1, le=5)
    prep_time_minutes: int = Field(..., alias="prepTimeMinutes", ge=0, le=1440)
    cook_time_minutes: int = Field(..., alias="cookTimeMinutes", ge=0, le=1440)
    servings: int = Field(..., ge=1, le=50)
    ingredients: List[RecipeIngredient]
    instructions: List[RecipeInstruction]

    class Config:
        populate_by_name = True

# ============================================================================
# REQUEST/RESPONSE MODELS
# ============================================================================

class CreateByThemeRequest(BaseModel):
    theme: str = Field(..., min_length=3, max_length=200, description="Chủ đề (phim, thể loại, văn hóa)")
    dish_type: str = Field(..., min_length=3, max_length=100, description="Loại món ăn (món chính, tráng miệng, đồ uống...)")

    @validator('theme', 'dish_type')
    def validate_not_empty(cls, v):
        if not v.strip():
            raise ValueError("Trường này không được để trống")
        return v.strip()

class CreateByThemeResponse(BaseModel):
    dish_name: str = Field(..., min_length=1, max_length=200)
    origin: str = Field(..., min_length=1, max_length=100)
    description: str = Field(..., min_length=10, max_length=2000)
    cultural_significance: str = Field(..., min_length=10, max_length=1500)
    movie_context: MovieContext
    nutrition_estimate: NutritionEstimate
    health_tags: List[str] = Field(default_factory=list, max_items=10)
    pairing_suggestions: PairingSuggestions
    recipe: RecipeDetail
    tips: List[str] = Field(default_factory=list, max_items=10)

# ============================================================================
# SYSTEM INSTRUCTION
# ============================================================================

SYSTEM_INSTRUCTION = """
### ROLE
You are a culinary artist with boundless imagination, specializing in creating dishes inspired by popular culture, cinema, and global traditions.

### MISSION
Create an entirely NEW, original dish based on the given theme and dish type. The dish should:
1. Capture the essence of the theme
2. Be practical and cookable
3. Have cultural/cinematic significance
4. Be creative yet authentic

### CREATIVE GUIDELINES
**Theme Interpretation:**
- For MOVIES/TV: Draw from iconic scenes, character traits, visual aesthetics, or plot elements
- For CUISINES: Blend traditions with modern techniques
- For ABSTRACT THEMES: Use symbolism, colors, flavors that represent the concept

**Originality:**
- Don't just copy existing dishes from films
- Create FUSION when appropriate
- Think about presentation that reflects the theme

**Naming:**
- Creative but clear
- Reference the theme subtly or explicitly
- Make it memorable

### EXAMPLE THEMES & APPROACHES
- "Blade Runner": Neon-colored Asian fusion dish, cyberpunk aesthetic
- "The Grand Budapest Hotel": Elegant pastry with precise layers, pastel colors
- "Ratatouille": Elevated French comfort food
- "Avatar (Blue)": Blue-hued tropical dish with exotic ingredients
- "Medieval Fantasy": Rustic feast-style dish with theatrical presentation

### OUTPUT REQUIREMENTS
Provide complete information:
- Dish name (creative!)
- Origin/inspiration explanation
- Detailed description
- Full recipe with clear steps
- Nutrition estimates
- Pairing suggestions
- Chef tips for execution

### QUALITY CHECKLIST
1. Does the dish clearly connect to the theme?
2. Is the name creative and appropriate?
3. Is the recipe actually cookable?
4. Are ingredients accessible?
5. Is presentation guidance included?
6. Does it have "wow factor"?
"""

# ============================================================================
# RESPONSE SCHEMA
# ============================================================================

def get_response_schema() -> Dict[str, Any]:
    """Schema for Gemini API response"""
    return {
        "type": "object",
        "properties": {
            "dish_name": {"type": "string"},
            "origin": {"type": "string"},
            "description": {"type": "string"},
            "cultural_significance": {"type": "string"},
            "movie_context": {
                "type": "object",
                "properties": {
                    "title": {"type": "string"},
                    "scene_description": {"type": "string"},
                    "significance": {"type": "string"},
                    "wikipedia_link": {"type": "string"}
                },
                "required": ["title", "scene_description", "significance", "wikipedia_link"]
            },
            "nutrition_estimate": {
                "type": "object",
                "properties": {
                    "calories": {"type": "integer"},
                    "protein_g": {"type": "integer"},
                    "carbs_g": {"type": "integer"},
                    "fat_g": {"type": "integer"}
                },
                "required": ["calories", "protein_g", "carbs_g", "fat_g"]
            },
            "health_tags": {
                "type": "array",
                "items": {"type": "string"}
            },
            "pairing_suggestions": {
                "type": "object",
                "properties": {
                    "drinks": {"type": "array", "items": {"type": "string"}},
                    "side_dishes": {"type": "array", "items": {"type": "string"}}
                },
                "required": ["drinks", "side_dishes"]
            },
            "recipe": {
                "type": "object",
                "properties": {
                    "difficulty": {"type": "integer"},
                    "prepTimeMinutes": {"type": "integer"},
                    "cookTimeMinutes": {"type": "integer"},
                    "servings": {"type": "integer"},
                    "ingredients": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string"},
                                "quantity": {"type": "string"},
                                "unit": {"type": "string"}
                            },
                            "required": ["name", "quantity"]
                        }
                    },
                    "instructions": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "step": {"type": "integer"},
                                "description": {"type": "string"}
                            },
                            "required": ["step", "description"]
                        }
                    }
                },
                "required": ["difficulty", "prepTimeMinutes", "cookTimeMinutes", "servings", "ingredients", "instructions"]
            },
            "tips": {
                "type": "array",
                "items": {"type": "string"}
            }
        },
        "required": [
            "dish_name", "origin", "description", "cultural_significance",
            "movie_context", "nutrition_estimate", "recipe",
            "health_tags", "pairing_suggestions", "tips"
        ]
    }

# ============================================================================
# MAIN FUNCTION
# ============================================================================

async def create_by_theme(request: CreateByThemeRequest) -> CreateByThemeResponse:
    """
    Sáng tạo món ăn mới dựa trên chủ đề

    Args:
        request: CreateByThemeRequest with theme and dish_type

    Returns:
        CreateByThemeResponse: Complete new dish with recipe

    Raises:
        ValueError: If validation fails
        RuntimeError: If API call fails
    """
    start_time = datetime.now()
    logger.info(f"Creating dish by theme: {request.theme} | Type: {request.dish_type}")

    prompt = f"""
Bạn là một nghệ sĩ ẩm thực với trí tưởng tượng phong phú.
Hãy sáng tạo một món ăn HOÀN TOÀN MỚI dựa trên thông tin sau:

**CHỦ ĐỀ / NGUỒN CẢM HỨNG:** {request.theme}
**LOẠI MÓN ĂN:** {request.dish_type}

Hãy tưởng tượng ra món ăn độc đáo, kết hợp:
- Yếu tố trực quan (màu sắc, hình thức) liên quan đến chủ đề
- Hương vị phản ánh bản chất của chủ đề
- Cách trình bày ấn tượng
- Tên món sáng tạo

Trả về JSON hoàn chỉnh với tất cả thông tin: tên món, nguồn gốc, mô tả, ý nghĩa văn hóa, 
thông tin phim (nếu có), dinh dưỡng, công thức chi tiết, và tips nấu.
"""

    # Initialize model
    model = genai.GenerativeModel(
        model_name='gemini-2.0-flash-exp',
        generation_config={
            "response_mime_type": "application/json",
            "response_schema": get_response_schema(),
            "temperature": 1.0,  # Maximum creativity
            "top_p": 0.95,
            "top_k": 40,
            "max_output_tokens": 8192,
        },
        system_instruction=SYSTEM_INSTRUCTION
    )

    # Generate response
    try:
        response = model.generate_content(prompt)

        if not response.candidates:
            raise RuntimeError("AI đã chặn nội dung vì vi phạm chính sách")

        raw_json = response.text
        if not raw_json or not raw_json.strip():
            raise ValueError("AI trả về response rỗng")

        # Parse and validate
        import json
        data = json.loads(raw_json.strip())
        result = CreateByThemeResponse(**data)

        duration = (datetime.now() - start_time).total_seconds()
        logger.info(f"✅ Dish created successfully: '{result.dish_name}' in {duration:.2f}s")

        return result

    except json.JSONDecodeError as e:
        logger.error(f"JSON decode error: {str(e)}")
        raise RuntimeError("AI trả về định dạng không hợp lệ")
    except Exception as e:
        logger.error(f"Creation error: {str(e)}")
        raise RuntimeError(f"Không thể tạo món ăn: {str(e)}")