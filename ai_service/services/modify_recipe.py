"""
ai_service/services/modify_recipe.py
Biến tấu công thức nấu ăn dựa trên yêu cầu người dùng
"""

import logging
from typing import Dict, Any
from datetime import datetime

from pydantic import BaseModel, Field, validator
import google.generativeai as genai

logger = logging.getLogger(__name__)

# ============================================================================
# PYDANTIC MODELS
# ============================================================================

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
    ingredients: list[RecipeIngredient]
    instructions: list[RecipeInstruction]

    class Config:
        populate_by_name = True

class ModifyRecipeRequest(BaseModel):
    original_recipe: RecipeDetail
    modification_request: str = Field(..., min_length=5, max_length=500)

    @validator('modification_request')
    def validate_request(cls, v):
        if not v.strip():
            raise ValueError("Yêu cầu biến tấu không được để trống")
        return v.strip()

class ModifyRecipeResponse(BaseModel):
    modified_recipe: RecipeDetail
    changes_summary: str = Field(..., description="Tóm tắt những thay đổi")

# ============================================================================
# SYSTEM INSTRUCTION
# ============================================================================

SYSTEM_INSTRUCTION = """
### ROLE
You are a creative and intelligent chef specializing in recipe adaptation and modification.

### MISSION
Modify the given recipe based on user's request while maintaining:
1. Dish authenticity and flavor balance
2. Practical cookability
3. Food safety standards
4. Nutritional considerations

### MODIFICATION GUIDELINES
**Common Requests:**
- Dietary restrictions (vegan, gluten-free, keto, halal, etc.)
- Ingredient substitutions (allergies, availability)
- Portion adjustments
- Cooking method changes (oven → air fryer)
- Difficulty level adjustments
- Time optimization

**Rules:**
1. **Preserve Essence**: Keep the core identity of the dish
2. **Explain Changes**: Provide clear reasoning for modifications
3. **Practical Substitutions**: Suggest realistic alternatives
4. **Proportional Adjustments**: Scale ingredients correctly
5. **Safety First**: Warn if modifications affect food safety

### OUTPUT FORMAT
Return JSON with:
- `modified_recipe`: Complete updated recipe
- `changes_summary`: Brief explanation of what changed and why

### QUALITY CHECKLIST
1. Are ingredient substitutions realistic?
2. Do cooking times make sense?
3. Is the modified recipe still cookable?
4. Are measurements proportional?
5. Is the difficulty level accurate?
"""

# ============================================================================
# RESPONSE SCHEMA
# ============================================================================

def get_response_schema() -> Dict[str, Any]:
    """Schema for Gemini API response"""
    return {
        "type": "object",
        "properties": {
            "modified_recipe": {
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
            "changes_summary": {"type": "string"}
        },
        "required": ["modified_recipe", "changes_summary"]
    }

# ============================================================================
# MAIN FUNCTION
# ============================================================================

async def modify_recipe(request: ModifyRecipeRequest) -> ModifyRecipeResponse:
    """
    Biến tấu công thức dựa trên yêu cầu người dùng

    Args:
        request: ModifyRecipeRequest containing original recipe and modification request

    Returns:
        ModifyRecipeResponse: Modified recipe with changes summary

    Raises:
        ValueError: If validation fails
        RuntimeError: If API call fails
    """
    start_time = datetime.now()
    logger.info(f"Starting recipe modification - Request: {request.modification_request}")

    # Prepare prompt
    original_recipe_json = request.original_recipe.model_dump_json(indent=2)

    prompt = f"""
Bạn là một đầu bếp sáng tạo. Hãy biến tấu công thức sau đây dựa trên yêu cầu của người dùng.

**YÊU CẦU BIẾN TẤU:**
{request.modification_request}

**CÔNG THỨC GỐC:**
```json
{original_recipe_json}
```

Hãy trả về JSON với công thức đã được điều chỉnh và giải thích ngắn gọn về những thay đổi.
Đảm bảo công thức mới vẫn thực tế và có thể nấu được.
"""

    # Initialize model
    model = genai.GenerativeModel(
        model_name='gemini-2.5-flash',
        generation_config={
            "response_mime_type": "application/json",
            "response_schema": get_response_schema(),
            "temperature": 0.8,  # Higher for creativity
            "top_p": 0.95,
            "max_output_tokens": 4096,
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
        result = ModifyRecipeResponse(**data)

        duration = (datetime.now() - start_time).total_seconds()
        logger.info(f"✅ Recipe modified successfully in {duration:.2f}s")

        return result

    except json.JSONDecodeError as e:
        logger.error(f"JSON decode error: {str(e)}")
        raise RuntimeError("AI trả về định dạng không hợp lệ")
    except Exception as e:
        logger.error(f"Modification error: {str(e)}")
        raise RuntimeError(f"Không thể biến tấu công thức: {str(e)}")