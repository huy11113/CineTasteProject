"""
ai_service/services/critique_dish.py
PHIÊN BẢN TỐI ƯU - Đầu bếp AI chuyên nghiệp đánh giá món ăn
"""

import logging
import json
from typing import List, Dict, Any
from datetime import datetime

from pydantic import BaseModel, Field, validator
import google.generativeai as genai

from .image_validator import ImageValidator

logger = logging.getLogger(__name__)

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
    critique: str = Field(..., min_length=100, max_length=3000, description="Nhận xét chuyên sâu của đầu bếp")
    score: float = Field(..., ge=0, le=100, description="Điểm tổng thể (0-100)")

    # Điểm chi tiết theo tiêu chí Michelin
    appearance_score: float = Field(..., ge=0, le=100, description="Điểm trình bày & thẩm mỹ")
    technique_score: float = Field(..., ge=0, le=100, description="Điểm kỹ thuật & chín")
    creativity_score: float = Field(..., ge=0, le=100, description="Điểm sáng tạo & cân bằng")

    # Phân tích chuyên sâu
    visual_analysis: str = Field(..., description="Phân tích trực quan chi tiết")
    technical_analysis: str = Field(..., description="Đánh giá kỹ thuật chế biến")

    strengths: List[str] = Field(..., min_items=2, max_items=5, description="Điểm xuất sắc")
    weaknesses: List[str] = Field(..., min_items=1, max_items=4, description="Điểm cần cải thiện")
    suggestions: List[str] = Field(..., min_items=3, max_items=6, description="Gợi ý nâng cao từ đầu bếp")

    # Đánh giá cấp độ
    level_assessment: str = Field(..., description="Home Cook / Restaurant Quality / Michelin Worthy")
    comparable_restaurant: str = Field(default="", description="So sánh với nhà hàng/món tương tự")


# ============================================================================
# SYSTEM INSTRUCTION - ĐỈNH CAO CULINARY EXPERTISE
# ============================================================================

SYSTEM_INSTRUCTION = """
You are an Executive Chef with 20+ years Michelin experience. Evaluate dishes with precision and constructive guidance.

## SCORING FRAMEWORK (0-100)

**APPEARANCE (0-100):**
- Composition: Rule of thirds, focal point, negative space, balance
- Color: Contrast, vibrancy, complementary palette
- Plating: Height/layering, saucing technique (drizzle/smear/dots), garnish placement
- Cleanliness: Rim, smudges, professional presentation
- Scale: 85-100 Michelin | 70-84 Restaurant | 50-69 Advanced Home | 30-49 Functional | 0-29 Poor

**TECHNIQUE (0-100):**
- Doneness: Color indicators, texture visible, moisture level, Maillard reaction
- Knife Skills: Uniform cuts (brunoise 3mm, julienne 3x3mm), clean edges
- Method: Appropriate cooking technique, temperature control
- Consistency: Crispy stays crispy, sauce viscosity (nappé standard), protein not dry
- Scale: 85-100 Perfect | 70-84 Professional | 50-69 Competent | 30-49 Flawed | 0-29 Failed

**CREATIVITY (0-100):**
- Originality: Innovation vs. traditional replication, signature elements
- Harmony: Logical ingredient pairing, portion ratios
- Balance: Protein:carb:veg ratio, rich + fresh elements, sauce not overwhelming
- Authenticity: Cultural respect + modern interpretation, seasonality
- Scale: 85-100 Signature | 70-84 Creative | 50-69 Safe | 30-49 Confused | 0-29 Illogical

## EVALUATION APPROACH

**Analysis Pattern:**
1. First impression - identify standout element
2. Visual breakdown - composition, colors, plating technique
3. Technical assessment - doneness, texture, knife work via visual cues
4. Creative evaluation - originality, balance, concept
5. Constructive feedback - 2-3 specific improvements with visual evidence
6. Encouragement - next level guidance

**Feedback Rules:**
- 60% praise, 40% constructive
- SPECIFIC citations: "Golden sear with visible Maillard at 75% surface" NOT "looks good"
- Use culinary terms naturally: Maillard, nappé, brunoise, jus, emulsion, quenelle
- Compare to real restaurants when applicable
- Tone: Professional mentor (demanding but fair)

**Common Issues to Spot:**
- Overcrowded plate / too empty (portion sizing)
- Uneven cuts (knife skills)
- Sauce pooling / rim smudges (hygiene)
- Color monotone / no contrast
- Overcooked (dark edges, curling) / undercooked (pale, raw appearance)
- Wilted garnish / non-edible decoration

## OUTPUT STRUCTURE (Vietnamese)

**critique** (200-500 words):
- Opening: "Nhìn vào món [dish], tôi thấy [standout feature]..."
- Visual: Detailed composition/color/plating analysis
- Technical: Doneness/texture assessment with evidence
- Constructive: "Để nâng tầm, hãy [specific action] vì [reason]..."
- Closing: Encouraging next-level guidance

**visual_analysis** (50-100 words): Composition, color palette, plating technique, garnish, cleanliness

**technical_analysis** (50-100 words): Doneness indicators, knife skills, cooking method, texture consistency

**strengths** (2-5 points): Specific excellent points with culinary terms
Example: "Lớp sear tạo vỏ ngoài hoàn hảo với phản ứng Maillard đồng đều ở 80% bề mặt"

**weaknesses** (1-4 points): Specific issues, not generic
Example: "Rau julienne dao động 2-5mm thay vì chuẩn 3mm, ảnh hưởng thẩm mỹ"

**suggestions** (3-6 tips): Actionable with reasoning
Example: "Thu nhỏ sauce 2-3 phút để đạt độ sệt nappé, tạo lớp phủ bóng chuyên nghiệp thay vì loãng như hiện tại"

**level_assessment**: Home Cook / Advanced Home / Restaurant Quality / Fine Dining Level

**comparable_restaurant**: Compare to real restaurant if applicable
Example: "Đạt chuẩn bistro bình dân, cần tinh chỉnh để lên level fine dining"

## KEY PRINCIPLES
1. Every critique MUST have visual evidence basis
2. Cite specific locations, colors, shapes - no vague comments
3. Balance honesty with encouragement
4. Use international standards but write naturally in Vietnamese
5. Professional respect - peer review, not teacher-student
"""

# ============================================================================
# RESPONSE SCHEMA
# ============================================================================

def get_response_schema() -> Dict[str, Any]:
    """Schema nâng cao cho Gemini API"""
    return {
        "type": "object",
        "properties": {
            "critique": {
                "type": "string",
                "description": "Nhận xét toàn diện 200-500 từ"
            },
            "score": {
                "type": "number",
                "description": "Điểm tổng thể 0-10"
            },
            "appearance_score": {"type": "number"},
            "technique_score": {"type": "number"},
            "creativity_score": {"type": "number"},

            "visual_analysis": {
                "type": "string",
                "description": "Phân tích trực quan chuyên sâu"
            },
            "technical_analysis": {
                "type": "string",
                "description": "Đánh giá kỹ thuật chuyên môn"
            },

            "strengths": {
                "type": "array",
                "items": {"type": "string"},
                "description": "2-5 điểm xuất sắc cụ thể"
            },
            "weaknesses": {
                "type": "array",
                "items": {"type": "string"},
                "description": "1-4 điểm cần cải thiện"
            },
            "suggestions": {
                "type": "array",
                "items": {"type": "string"},
                "description": "3-6 gợi ý nâng cao"
            },

            "level_assessment": {
                "type": "string",
                "description": "Đánh giá cấp độ"
            },
            "comparable_restaurant": {
                "type": "string",
                "description": "So sánh với nhà hàng thực tế"
            }
        },
        "required": [
            "critique", "score",
            "appearance_score", "technique_score", "creativity_score",
            "visual_analysis", "technical_analysis",
            "strengths", "weaknesses", "suggestions",
            "level_assessment"
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
    Đánh giá món ăn bởi Executive Chef AI

    Args:
        file_data: Dữ liệu hình ảnh
        mime_type: Loại file (image/jpeg, image/png, etc.)
        dish_name: Tên món ăn

    Returns:
        CritiqueDishResponse: Phân tích chuyên sâu với scoring chi tiết

    Raises:
        ValueError: Lỗi validation
        RuntimeError: Lỗi API hoặc xử lý
    """
    start_time = datetime.now()
    logger.info(f"🔍 Executive Chef đang đánh giá: {dish_name}")

    # Validate và tối ưu hình ảnh
    ImageValidator.validate_file(file_data, mime_type)
    pil_image = ImageValidator.optimize_image(file_data)

    # Prompt chi tiết cho AI
    prompt = f"""
Với tư cách một **Executive Chef chuyên nghiệp**, hãy đánh giá món: **{dish_name}**

📋 **YÊU CẦU ĐÁNH GIÁ:**

**1. VISUAL ANALYSIS (50-100 từ):**
- Phân tích composition (rule of thirds, focal point, balance)
- Color palette và contrast
- Plating technique (height, layering, saucing)
- Garnish appropriateness và placement
- Plate cleanliness và presentation hygiene
- So sánh với restaurant standards

**2. TECHNICAL ANALYSIS (50-100 từ):**
- Doneness indicators (color, texture, moisture)
- Knife skills (uniform cuts, clean edges)
- Cooking method appropriateness
- Texture consistency visible
- Potential issues (overcook, undercook, poor prep)
- Execution level (home cook vs. professional)

**3. COMPREHENSIVE CRITIQUE (200-500 từ):**
Theo structure:
- Opening: Ấn tượng đầu tiên, highlight điểm mạnh nhất
- Visual breakdown: Chi tiết về plating, composition, aesthetics
- Technical assessment: Đánh giá kỹ thuật qua visual cues
- Constructive feedback: 2-3 điểm cải thiện CỤ THỂ với lý do
- Professional closing: Động viên và định hướng level tiếp theo

**4. SCORING (0-10, có thể dùng .5):**
- Appearance: Đánh giá thẩm mỹ, plating
- Technique: Đánh giá kỹ thuật execution
- Creativity: Đánh giá innovation, balance
- Overall: Tổng hợp (không phải trung bình cộng)

**5. DETAILED FEEDBACK:**
- **Strengths (2-5 điểm)**: Điểm xuất sắc CỤ THỂ (vd: "Perfect sear với Maillard reaction visible")
- **Weaknesses (1-4 điểm)**: Điểm cần cải thiện, KHÔNG chung chung
- **Suggestions (3-6 gợi ý)**: Actionable advice với lý do (vd: "Reduce sauce 2-3 phút để đạt nappé consistency")

**6. LEVEL ASSESSMENT:**
- Đánh giá: Home Cook / Advanced Home / Restaurant Quality / Fine Dining Level
- So sánh với nhà hàng cụ thể nếu có (vd: "Đạt level Bistro X, gần đạt The Restaurant Y")

---

⚠️ **QUAN TRỌNG:**
- Dùng thuật ngữ culinary chuyên nghiệp (Maillard, nappé, brunoise, jus...)
- CITE CỤ THỂ: "vì màu vàng đều" thay vì "đẹp"
- Tone = Executive Chef mentoring sous chef (professional, fair, demanding)
- Balance 60% praise / 40% constructive
- Mọi critique phải có CƠ SỞ visual evidence

Hãy đánh giá với tiêu chuẩn nhà hàng quốc tế, nhưng động viên để học viên tiến bộ.
"""

    # Khởi tạo Gemini model
    model = genai.GenerativeModel(
        model_name='gemini-2.5-pro',  # Gemini 2.5 Flash với thinking capability
        generation_config={
            "response_mime_type": "application/json",
            "response_schema": get_response_schema(),
            "temperature": 0.8,  # Tăng creativity
            "top_p": 0.95,
            "top_k": 40,
            "max_output_tokens": 8192,  # Tăng để phân tích chi tiết hơn
        },
        system_instruction=SYSTEM_INSTRUCTION
    )

    # Generate critique
    try:
        response = model.generate_content([prompt, pil_image])

        if not response.candidates:
            raise RuntimeError("AI đã chặn nội dung. Vui lòng kiểm tra hình ảnh.")

        raw_json = response.text
        if not raw_json or not raw_json.strip():
            raise ValueError("AI không trả về phản hồi")

        # Parse JSON và validate
        data = json.loads(raw_json.strip())
        result = CritiqueDishResponse(**data)

        duration = (datetime.now() - start_time).total_seconds()
        logger.info(
            f"✅ Chef's Verdict: {result.score}/100 | "
            f"Level: {result.level_assessment} | "
            f"Time: {duration:.2f}s"
        )

        return result

    except json.JSONDecodeError as e:
        logger.error(f"❌ JSON parse error: {str(e)}")
        if 'raw_json' in locals():
            logger.error(f"Raw response: {raw_json[:500]}")
        raise RuntimeError("AI trả về định dạng không hợp lệ")

    except Exception as e:
        logger.error(f"❌ Critique failed: {str(e)}")
        raise RuntimeError(f"Không thể đánh giá món ăn: {str(e)}")


# ============================================================================
# HELPER FUNCTIONS (Optional)
# ============================================================================

def format_critique_for_display(critique: CritiqueDishResponse) -> str:
    """
    Format critique thành text đẹp để hiển thị

    Returns:
        Formatted string với emoji và structure rõ ràng
    """
    output = f"""
{'='*60}
🍽️  ĐÁNH GIÁ CỦA EXECUTIVE CHEF
{'='*60}

📊 TỔNG ĐIỂM: {critique.score}/100
   └─ Trình bày: {critique.appearance_score}/100
   └─ Kỹ thuật:  {critique.technique_score}/100
   └─ Sáng tạo:  {critique.creativity_score}/100

📝 CẤP ĐỘ: {critique.level_assessment}
{f'🏆 So sánh: {critique.comparable_restaurant}' if critique.comparable_restaurant else ''}

{'─'*60}
💬 NHẬN XÉT CỦA ĐẦU BẾP:
{critique.critique}

{'─'*60}
👁️ PHÂN TÍCH TRỰC QUAN:
{critique.visual_analysis}

{'─'*60}
🔧 PHÂN TÍCH KỸ THUẬT:
{critique.technical_analysis}

{'─'*60}
✅ ĐIỂM MẠNH:
"""
    for i, strength in enumerate(critique.strengths, 1):
        output += f"   {i}. {strength}\n"

    output += f"\n{'─'*60}\n⚠️ ĐIỂM CẦN CẢI THIỆN:\n"
    for i, weakness in enumerate(critique.weaknesses, 1):
        output += f"   {i}. {weakness}\n"

    output += f"\n{'─'*60}\n💡 GỢI Ý TỪ ĐẦU BẾP:\n"
    for i, suggestion in enumerate(critique.suggestions, 1):
        output += f"   {i}. {suggestion}\n"

    output += f"\n{'='*60}\n"

    return output