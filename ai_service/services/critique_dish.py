"""
ai_service/services/critique_dish.py
Nhận xét, chấm điểm và đưa ra gợi ý cải thiện món ăn
"""

import os
import io
import logging
from typing import List, Dict, Any
from datetime import datetime

from PIL import Image
from pydantic import BaseModel, Field, validator
import google.generativeai as genai

logger = logging.getLogger(__name__)

# Import image validator from analyze_dish
from .analyze_dish import ImageValidator

# ============================================================================
# PYDANTIC MODELS
# ============================================================================

class CritiqueDishRequest(BaseModel):
    dish_name: str = Field(..., min_length=1, max_length=200)

    @validator('dish_name')
    def validate_name(cls, v):
        if not v.strip():
            raise ValueError("Tên món ăn không được để trống")
        return v.strip()

class CritiqueDishResponse(BaseModel):
    critique: str = Field(..., min_length=50, max_length=2000, description="Nhận xét chi tiết")
    score: float = Field(..., ge=0, le=10, description="Điểm đánh giá từ 0-10")
    suggestions: List[str] = Field(..., min_items=1, max_items=10, description="Gợi ý cải thiện")

    # Additional fields for detailed feedback
    appearance_score: float = Field(..., ge=0, le=10, description="Điểm trình bày")
    technique_score: float = Field(..., ge=0, le=10, description="Điểm kỹ thuật")
    creativity_score: float = Field(..., ge=0, le=10, description="Điểm sáng tạo")

    strengths: List[str] = Field(default_factory=list, max_items=5, description="Điểm mạnh")
    weaknesses: List[str] = Field(default_factory=list, max_items=5, description="Điểm cần cải thiện")

# ============================================================================
# SYSTEM INSTRUCTION
# ============================================================================

SYSTEM_INSTRUCTION = """
### ROLE
You are a supportive and constructive culinary mentor, similar to a kind judge on a cooking show. Your feedback should be:
- **Encouraging**: Always start with genuine praise
- **Constructive**: Point out improvements kindly
- **Educational**: Explain WHY something works or doesn't
- **Motivating**: End with encouragement

### EVALUATION CRITERIA
**1. Appearance (Visual Appeal) - 0-10:**
- Plating and presentation
- Color balance and contrast
- Garnish appropriateness
- Overall visual appeal

**2. Technique (Execution) - 0-10:**
- Cooking method appropriateness
- Ingredient preparation
- Timing indicators
- Texture visible quality

**3. Creativity (Innovation) - 0-10:**
- Originality of presentation
- Ingredient combinations
- Modern interpretations
- Artistic expression

### SCORING GUIDELINES
**9-10**: Professional restaurant quality, exceptional
**7-8**: Very good home cooking, impressive
**5-6**: Decent effort, room for improvement
**3-4**: Needs work, but shows potential
**1-2**: Significant issues to address
**0**: Cannot evaluate or not food

### FEEDBACK STRUCTURE
1. **Opening Praise**: Start with 1-2 genuine compliments
2. **Analysis**: Discuss appearance, technique, creativity
3. **Constructive Points**: 2-3 specific improvements (kind tone)
4. **Encouragement**: End with motivating statement

### TONE EXAMPLES
❌ "The plating is messy and unprofessional"
✅ "I love your enthusiasm! To make it even better, try using a smaller plate to create a more focused presentation"

❌ "The vegetables are overcooked"
✅ "Great choice of vegetables! Next time, try reducing cooking time by 2-3 minutes to keep that beautiful color and crunch"

### OUTPUT REQUIREMENTS
- `critique`: Full written feedback (50-300 words)
- `score`: Overall score 0-10 (can use decimals like 7.5)
- `appearance_score`, `technique_score`, `creativity_score`: Individual scores
- `strengths`: 3-5 positive points
- `weaknesses`: 1-3 areas for improvement (phrase positively)
- `suggestions`: 3-5 actionable tips
"""

# ============================================================================
# RESPONSE SCHEMA
# ============================================================================

def get_response_schema() -> Dict[str, Any]:
    """Schema for Gemini API response"""
    return {
        "type": "object",
        "properties": {
            "critique": {"type": "string"},
            "score": {"type": "number"},
            "appearance_score": {"type": "number"},
            "technique_score": {"type": "number"},
            "creativity_score": {"type": "number"},
            "strengths": {
                "type": "array",
                "items": {"type": "string"}
            },
            "weaknesses": {
                "type": "array",
                "items": {"type": "string"}
            },
            "suggestions": {
                "type": "array",
                "items": {"type": "string"}
            }
        },
        "required": [
            "critique", "score",
            "appearance_score", "technique_score", "creativity_score",
            "strengths", "weaknesses", "suggestions"
        ]
    }

# ============================================================================
# MAIN FUNCTION
# ============================================================================

async def critique_dish(
        file_data: bytes,
        mime_type: str,
        dish_name: str
) -> CritiqueDishResponse:
    """
    Nhận xét và chấm điểm món ăn từ hình ảnh

    Args:
        file_data: Image file bytes
        mime_type: MIME type of image
        dish_name: Name of the dish being critiqued

    Returns:
        CritiqueDishResponse: Detailed critique with scores

    Raises:
        ValueError: If validation fails
        RuntimeError: If API call fails
    """
    start_time = datetime.now()
    logger.info(f"Critiquing dish: {dish_name}")

    # Validate and optimize image
    ImageValidator.validate_file(file_data, mime_type)
    pil_image = ImageValidator.optimize_image(file_data)

    prompt = f"""
Bạn là một người hướng dẫn nấu ăn thân thiện và tích cực (mentor).
Hãy phân tích hình ảnh thành phẩm của món: **{dish_name}**

Viết nhận xét chi tiết theo cấu trúc:
1. **Ưu điểm**: Bắt đầu bằng lời khen chân thành về điểm nổi bật
2. **Phân tích**: Đánh giá trình bày, kỹ thuật, sáng tạo
3. **Điểm cần cải thiện**: Chỉ ra 1-3 điểm có thể cải thiện (nhẹ nhàng, xây dựng)
4. **Khích lệ**: Kết thúc bằng lời động viên

Chấm điểm:
- **Overall Score** (0-10): Tổng thể
- **Appearance** (0-10): Trình bày thẩm mỹ
- **Technique** (0-10): Kỹ thuật thực hiện
- **Creativity** (0-10): Tính sáng tạo

Cung cấp:
- Điểm mạnh (strengths): 3-5 điểm tốt
- Điểm yếu (weaknesses): 1-3 điểm cần cải thiện
- Gợi ý (suggestions): 3-5 lời khuyên cụ thể

Giữ tone thân thiện, động viên!
"""

    # Initialize model
    model = genai.GenerativeModel(
        model_name='gemini-2.0-flash-exp',
        generation_config={
            "response_mime_type": "application/json",
            "response_schema": get_response_schema(),
            "temperature": 0.7,
            "top_p": 0.95,
            "max_output_tokens": 4096,
        },
        system_instruction=SYSTEM_INSTRUCTION
    )

    # Generate response
    try:
        response = model.generate_content([prompt, pil_image])

        if not response.candidates:
            raise RuntimeError("AI đã chặn nội dung vì vi phạm chính sách")

        raw_json = response.text
        if not raw_json or not raw_json.strip():
            raise ValueError("AI trả về response rỗng")

        # Parse and validate
        import json
        data = json.loads(raw_json.strip())
        result = CritiqueDishResponse(**data)

        duration = (datetime.now() - start_time).total_seconds()
        logger.info(f"✅ Critique completed - Score: {result.score}/10 in {duration:.2f}s")

        return result

    except json.JSONDecodeError as e:
        logger.error(f"JSON decode error: {str(e)}")
        raise RuntimeError("AI trả về định dạng không hợp lệ")
    except Exception as e:
        logger.error(f"Critique error: {str(e)}")
        raise RuntimeError(f"Không thể đánh giá món ăn: {str(e)}")