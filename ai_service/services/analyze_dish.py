"""
ai_service/services/analyze_dish.py
FIXED VERSION - Sử dụng ImageValidator từ file riêng
"""

import os
import json
import logging
from typing import Optional, Dict, Any, List
from datetime import datetime

from pydantic import BaseModel, Field
import google.generativeai as genai

# ============================================================================
# IMPORT IMAGE VALIDATOR TỪ FILE RIÊNG
# ============================================================================
from .image_validator import ImageValidator

# ============================================================================
# CONFIGURATION & CONSTANTS
# ============================================================================

API_KEY = os.getenv('GOOGLE_API_KEY')
MAX_RETRIES = 3

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

if not API_KEY:
    raise ValueError("GOOGLE_API_KEY is not set in environment variables")

genai.configure(api_key=API_KEY)

# ============================================================================
# PYDANTIC MODELS (Response Schema)
# ============================================================================

class MovieContext(BaseModel):
    title: str = Field(..., description="Tên đầy đủ của phim/show")
    scene_description: str = Field(..., description="Mô tả cảnh xuất hiện món ăn")
    significance: str = Field(..., description="Ý nghĩa của món ăn trong phim")
    wikipedia_link: str = Field(default="", description="URL Wikipedia của phim")

class NutritionEstimate(BaseModel):
    calories: int = Field(..., ge=0, le=5000)
    protein_g: int = Field(..., ge=0, le=500, alias="protein_g")
    carbs_g: int = Field(..., ge=0, le=1000, alias="carbs_g")
    fat_g: int = Field(..., ge=0, le=500, alias="fat_g")

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

class AnalyzeDishResponse(BaseModel):
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
You are "Chef Gemini", an AI culinary expert, food historian, and passionate cinephile with deep knowledge of global cuisine and cinematic history.

### MISSION
Analyze the provided image and user context (from film/TV) and return a comprehensive JSON object that strictly adheres to the provided schema. Bridge the worlds of food and film with rich narrative.

### CRITICAL RULES
1. **FILM ANALYSIS IS PARAMOUNT**: Identify the movie/show using visual cues and user hints. This is the MOST important part.
2. **WIKIPEDIA LINK INTEGRITY**: Only provide valid, full Wikipedia URLs. If uncertain, return empty string "". DO NOT GUESS.
3. **HANDLING UNCERTAINTY**: If uncertain about dish/film, reduce detail and acknowledge uncertainty. Better general and correct than specific and wrong.
4. **NON-FOOD PROTOCOL**: If image is clearly not food, set dish_name to "Không phải món ăn" and explain in description.
5. **SCHEMA ADHERENCE**: Strict compliance with JSON schema. All numerical fields must be integers.
6. **PURE JSON OUTPUT**: Return ONLY the JSON object. No explanatory text, comments, or markdown fences.

### RECIPE & NUTRITION GUIDELINES
1. **RECIPE CLARITY**:
   - Use standard metric/imperial units (grams, ml, cups, tbsp)
   - Keep quantities precise
   - Instructions must be concise, actionable, direct
   - Ensure recipe is cookable based on visual evidence

2. **NUTRITIONAL ACCURACY**:
   - Provide realistic estimates based on standard serving size
   - Avoid generic placeholder numbers
   - Ensure macronutrients align with total calories (Protein*4 + Carbs*4 + Fat*9 ≈ Calories)

### THINKING PROCESS
1. **Image Analysis**: Scrutinize dish, ingredients, cooking style, presentation, background details
2. **Context Analysis**: Extract keywords from user context about film titles, characters, scenes
3. **Information Synthesis**: Connect dish with film scene and narrative
4. **Knowledge Retrieval**: Find dish's origin, cultural history
5. **Recipe & Nutrition**: Draft practical recipe with realistic nutritional values
6. **Link Verification**: Search for official Wikipedia page. If found, include. Else, empty string.
7. **Quality Check**: Run through checklist before finalizing

### 9-POINT QUALITY CHECKLIST
1. Is `dish_name` accurate and specific?
2. Is `movie_context.title` correct?
3. Is `wikipedia_link` valid URL or correctly empty ""?
4. Are ALL numerical fields actual integers?
5. Is `description` vivid and detailed?
6. Is `recipe` concise and practical?
7. Do macros roughly sum to calories?
8. Are all 'required' fields present?
9. Is output pure JSON without extra text?
"""

# ============================================================================
# RESPONSE SCHEMA FOR GEMINI
# ============================================================================

def get_response_schema() -> Dict[str, Any]:
    """Generate response schema for Gemini API"""
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
# MAIN ANALYSIS FUNCTION
# ============================================================================

async def analyze_dish_from_image(
        file_data: bytes,
        mime_type: str,
        context: Optional[str] = None
) -> AnalyzeDishResponse:
    """
    Main function to analyze dish from image

    Args:
        file_data: Image file bytes
        mime_type: MIME type of image
        context: Optional context from user about film/scene

    Returns:
        AnalyzeDishResponse: Structured analysis result

    Raises:
        ValueError: If validation fails
        RuntimeError: If API calls fail after retries
    """
    start_time = datetime.now()
    logger.info(f"Starting dish analysis - Context length: {len(context) if context else 0}")

    # Validate and optimize image
    ImageValidator.validate_file(file_data, mime_type)
    pil_image = ImageValidator.optimize_image(file_data)

    # Prepare prompt
    user_context = context.strip() if context else "Người dùng không cung cấp bối cảnh bổ sung."
    user_prompt = f"""Dựa vào hình ảnh và thông tin sau, hãy tạo ra đối tượng JSON hoàn chỉnh.

THÔNG TIN TỪ NGƯỜI DÙNG:
{user_context}"""

    # Initialize model
    model = genai.GenerativeModel(
        model_name='gemini-2.5-pro',
        generation_config={
            "response_mime_type": "application/json",
            "response_schema": get_response_schema(),
            "temperature": 0.7,
            "top_p": 0.95,
            "top_k": 40,
            "max_output_tokens": 8192,
        },
        system_instruction=SYSTEM_INSTRUCTION
    )

    # Retry loop
    last_error = None
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            logger.info(f"🔄 Attempt {attempt}/{MAX_RETRIES}")

            # Generate content
            response = model.generate_content([user_prompt, pil_image])

            # Check for safety blocks
            if not response.candidates:
                raise RuntimeError("SAFETY_BLOCK: Nội dung đã bị chặn vì vi phạm chính sách an toàn.")

            raw_json = response.text
            if not raw_json or not raw_json.strip():
                raise ValueError("VALIDATION_ERROR: AI trả về response rỗng.")

            # Parse JSON
            logger.info("📝 Parsing JSON response")
            data = json.loads(raw_json.strip())

            # Validate with Pydantic
            result = AnalyzeDishResponse(**data)

            duration = (datetime.now() - start_time).total_seconds()
            logger.info(f"✅ Analysis successful in {duration:.2f}s")

            return result

        except json.JSONDecodeError as e:
            last_error = e
            logger.error(f"❌ JSON decode error: {str(e)}")

        except Exception as e:
            last_error = e
            logger.error(f"❌ Attempt {attempt} failed: {str(e)}")

            # Don't retry on validation errors
            if "VALIDATION_ERROR" in str(e) or "SAFETY_BLOCK" in str(e):
                raise RuntimeError(str(e))

        # Exponential backoff
        if attempt < MAX_RETRIES:
            import asyncio
            wait_time = 2 ** (attempt - 1)
            logger.info(f"⏳ Waiting {wait_time}s before retry...")
            await asyncio.sleep(wait_time)

    # All retries failed
    raise RuntimeError(
        f"Không thể phân tích món ăn sau {MAX_RETRIES} lần thử. "
        f"Lỗi: {str(last_error)}"
    )