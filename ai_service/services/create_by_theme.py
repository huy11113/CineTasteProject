"""
ai_service/services/create_by_theme.py
Creative Chef - Sáng tạo món ăn với phong cách kể chuyện điện ảnh
(Phiên bản đầy đủ từ creative_chef.py)
"""

import logging
import os
import json
from typing import Dict, Any, List
from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field, validator
import google.generativeai as genai

# --- CẤU HÌNH API KEY (Thêm phần này để code chạy được) ---
try:
    from ..config import AIConfig
    API_KEY = AIConfig.GOOGLE_API_KEY
except ImportError:
    API_KEY = os.getenv('GOOGLE_API_KEY')

if API_KEY:
    genai.configure(api_key=API_KEY)

logger = logging.getLogger(__name__)

# ============================================================================
# ENUMS
# ============================================================================

class NarrativeStyle(str, Enum):
    COMIC_MODE = "Comic Mode"
    MYSTIC_WHISPER = "Mystic Whisper"
    ACTION_RUSH = "Action Rush"
    GHIBLI_SOFT_DREAM = "Ghibli Soft Dream"
    CYBERPUNK_LOGIC = "Cyberpunk Logic"
    ROMANCE_MOOD = "Romance Mood"
    DRAMA_DEEP = "Drama Deep"
    DEFAULT = "Standard"

# ============================================================================
# REQUEST MODEL
# ============================================================================

class CreativeChefRequest(BaseModel):
    inspiration: str = Field(..., min_length=3, max_length=200, description="Tên phim/anime/chủ đề")
    mood: str = Field(default="Normal", description="Không khí phim")
    ingredients: str = Field(default="", description="Nguyên liệu có sẵn")
    diet: str = Field(default="None", description="Chế độ ăn")
    creativity: int = Field(default=50, ge=0, le=100, description="Mức độ sáng tạo 0-100")
    time: str = Field(default="medium", description="fast/medium/slow")
    difficulty: str = Field(default="medium", description="easy/medium/hard")
    dining_style: str = Field(default="Cinematic", description="Phong cách ăn uống")
    skill_level: str = Field(default="Medium", description="Trình độ nấu ăn")

    @validator('inspiration')
    def validate_inspiration(cls, v):
        if not v.strip():
            raise ValueError("Cần có nguồn cảm hứng")
        return v.strip()

# ============================================================================
# RESPONSE MODELS
# ============================================================================

class FlavorProfile(BaseModel):
    sweet: int = Field(default=0, ge=0, le=10)
    sour: int = Field(default=0, ge=0, le=10)
    spicy: int = Field(default=0, ge=0, le=10)
    umami: int = Field(default=0, ge=0, le=10)
    richness: int = Field(default=0, ge=0, le=10)

class Macros(BaseModel):
    calories: str = Field(default="0")
    protein: str = Field(default="0g")
    carbs: str = Field(default="0g")
    fat: str = Field(default="0g")

class CreativeChefResponse(BaseModel):
    # Core Info
    recipeName: str = Field(..., description="Tên món ăn sáng tạo")
    narrativeStyle: str = Field(default="Standard", description="Phong cách kể chuyện")
    story: str = Field(..., description="Câu chuyện món ăn")
    connection: str = Field(default="", description="Lời bình đạo diễn")

    # Recipe Details (Simplified)
    ingredients: List[str] = Field(default_factory=list, description="Danh sách nguyên liệu dạng text")
    instructions: List[str] = Field(default_factory=list, description="Các bước làm dạng text")

    # Time & Specs
    prepTime: str = Field(default="??", description="Thời gian sơ chế")
    cookTime: str = Field(default="??", description="Thời gian nấu")

    # Sensory & Aesthetics
    flavorProfile: FlavorProfile
    visualColors: List[str] = Field(default_factory=list, description="Mã màu hex")
    platingGuide: str = Field(default="", description="Hướng dẫn trình bày")

    # Extras
    pairing: str = Field(default="", description="Gợi ý đồ uống/món phụ")
    musicRecommendation: str = Field(default="Silence", description="Nhạc nền")
    macros: Macros
    origin: str = Field(default="", description="Nguồn gốc món ăn")

# ============================================================================
# COLOR PALETTE GENERATOR
# ============================================================================

def generate_color_palette(style: str, mood: str) -> List[str]:
    """Tạo bảng màu hex dựa trên style và mood"""

    # Normalize
    s = str(style).lower().replace(' ', '').replace('_', '')
    m = str(mood).lower()

    color_map = {
        # Action & Thriller
        'actionrush': ['#8B0000', '#DC143C', '#1A1A1A'],
        'action': ['#B22222', '#FF4500', '#2C2C2C'],

        # Horror & Dark
        'horror': ['#2F0000', '#660000', '#0A0A0A'],
        'noir': ['#1A1A1A', '#4A4A4A', '#8B0000'],

        # Sci-Fi & Cyberpunk
        'cyberpunklogic': ['#0A192F', '#64FFDA', '#8B5CF6'],
        'cyberpunk': ['#0F172A', '#06B6D4', '#A855F7'],
        'scifi': ['#1E293B', '#3B82F6', '#10B981'],

        # Ghibli & Nature
        'ghiblisoftdream': ['#2D5016', '#7CB342', '#81D4FA'],
        'ghibli': ['#1B5E20', '#8BC34A', '#4FC3F7'],
        'nature': ['#1B5E20', '#66BB6A', '#FFD54F'],

        # Mystic & Fantasy
        'mysticwhisper': ['#4A148C', '#9C27B0', '#E91E63'],
        'mystic': ['#311B92', '#7E57C2', '#EC407A'],
        'fantasy': ['#4A148C', '#AB47BC', '#F48FB1'],

        # Comedy & Fun
        'comicmode': ['#F57F17', '#FBC02D', '#1565C0'],
        'comic': ['#F9A825', '#FDD835', '#0277BD'],
        'comedy': ['#FF6F00', '#FFEB3B', '#0288D1'],

        # Romance
        'romancemood': ['#880E4F', '#E91E63', '#FCE4EC'],
        'romance': ['#C2185B', '#F06292', '#F8BBD0'],

        # Drama
        'dramadeep': ['#1A237E', '#303F9F', '#5C6BC0'],
        'drama': ['#263238', '#455A64', '#78909C'],
    }

    # Match style
    for key, colors in color_map.items():
        if key in s:
            return colors

    # Match mood
    mood_map = {
        'adventure': ['#FF6B35', '#F7931E', '#004E89'],
        'horror': ['#2F0000', '#660000', '#0A0A0A'],
        'romance': ['#C2185B', '#F06292', '#F8BBD0'],
        'comedy': ['#FFC107', '#FF9800', '#03A9F4'],
    }

    for key, colors in mood_map.items():
        if key in m:
            return colors

    # Default
    return ['#0F172A', '#1E293B', '#475569']

# ============================================================================
# SYSTEM INSTRUCTION
# ============================================================================

SYSTEM_INSTRUCTION = """
### IDENTITY
You are a CINEMATIC CULINARY STORYTELLER - part chef, part screenwriter, part artist.

### MISSION
Transform a film/anime/theme into a complete sensory experience through food.
This is NOT just a recipe - it's a NARRATIVE EXPERIENCE.

### CREATIVE FRAMEWORK

**1. NARRATIVE VOICE (Choose based on mood/genre):**
- **Comic Mode**: Playful, witty narration (think Deadpool cooking)
- **Mystic Whisper**: Poetic, mystical language (Studio Ghibli vibes)
- **Action Rush**: Fast-paced, energetic, intense (like a heist movie)
- **Ghibli Soft Dream**: Gentle, nostalgic, nature-focused
- **Cyberpunk Logic**: Technical, futuristic, precise
- **Romance Mood**: Sensual, emotional, intimate
- **Drama Deep**: Serious, profound, thoughtful

**2. STORY STRUCTURE:**
- **Opening (story)**: Set the scene - why this dish exists in this universe
- **Connection**: Director's commentary - the philosophy behind the dish
- **Instructions**: Written like ACTION SCENES, not boring steps

**3. SENSORY DESIGN:**
- **Flavor Profile**: Rate 0-10 for sweet/sour/spicy/umami/richness
- **Visual Colors**: 3 HEX colors that represent the film's palette
- **Plating**: Describe like a movie scene composition

**4. CREATIVITY LEVELS:**
- 0-30: Stay faithful to authentic recipes
- 30-70: Creative fusion, modern twists
- 70-100: Experimental, avant-garde, molecular gastronomy

### OUTPUT RULES
1. Recipe name should be CINEMATIC (not just "Pasta")
2. Story must connect emotionally to the theme
3. Ingredients list is simple strings (not objects)
4. Instructions are narrative, not robotic
5. Always include music recommendation from the film/similar
6. Visual colors MUST be valid HEX codes (#RRGGBB)

### EXAMPLE TRANSFORMATION
**Input:** Spirited Away, Comfort Food
**Bad:** "Onigiri rice balls - Step 1: Cook rice..."
**Good:** - Name: "Chihiro's Courage Onigiri"
- Story: "In the spirit world's bathhouse, a simple rice ball became..."
- Instructions: "As steam rises like spirits awakening, shape the warm rice..."
- Colors: ["#2D5016", "#7CB342", "#81D4FA"] (forest greens, sky blue)
"""

# ============================================================================
# RESPONSE SCHEMA
# ============================================================================

def get_response_schema() -> Dict[str, Any]:
    """Schema for Gemini structured output"""
    return {
        "type": "object",
        "properties": {
            "recipeName": {"type": "string"},
            "narrativeStyle": {"type": "string"},
            "story": {"type": "string"},
            "connection": {"type": "string"},
            "ingredients": {
                "type": "array",
                "items": {"type": "string"}
            },
            "instructions": {
                "type": "array",
                "items": {"type": "string"}
            },
            "prepTime": {"type": "string"},
            "cookTime": {"type": "string"},
            "flavorProfile": {
                "type": "object",
                "properties": {
                    "sweet": {"type": "integer"},
                    "sour": {"type": "integer"},
                    "spicy": {"type": "integer"},
                    "umami": {"type": "integer"},
                    "richness": {"type": "integer"}
                },
                "required": ["sweet", "sour", "spicy", "umami", "richness"]
            },
            "visualColors": {
                "type": "array",
                "items": {"type": "string"}
            },
            "platingGuide": {"type": "string"},
            "pairing": {"type": "string"},
            "musicRecommendation": {"type": "string"},
            "macros": {
                "type": "object",
                "properties": {
                    "calories": {"type": "string"},
                    "protein": {"type": "string"},
                    "carbs": {"type": "string"},
                    "fat": {"type": "string"}
                },
                "required": ["calories", "protein", "carbs", "fat"]
            },
            "origin": {"type": "string"}
        },
        "required": [
            "recipeName", "narrativeStyle", "story", "ingredients",
            "instructions", "prepTime", "cookTime", "flavorProfile",
            "visualColors", "platingGuide", "macros"
        ]
    }

# ============================================================================
# MAIN FUNCTION
# ============================================================================

async def create_by_theme(request: CreativeChefRequest) -> CreativeChefResponse:
    """
    Tạo món ăn với phong cách kể chuyện điện ảnh

    Args:
        request: CreativeChefRequest với đầy đủ tham số

    Returns:
        CreativeChefResponse: Món ăn với narrative đầy đủ
    """
    start_time = datetime.now()
    logger.info(f"🎬 Creative Chef: {request.inspiration} | Mood: {request.mood} | Creativity: {request.creativity}%")

    # Build dynamic prompt
    prompt = f"""
🎬 **NHIỆM VỤ SÁNG TẠO**

**CẢM HỨNG:** {request.inspiration}
**KHÔNG KHÍ:** {request.mood}
**ĐỘ SÁNG TẠO:** {request.creativity}/100 (0=trung thành nguyên gốc, 100=thử nghiệm táo bạo)
**THỜI GIAN CHẾ BIẾN:** {request.time}
**ĐỘ KHÓ:** {request.difficulty}
**CHỂ ĐỘ ĂN:** {request.diet}
{"**NGUYÊN LIỆU CÓ SẴN:** " + request.ingredients if request.ingredients else ""}

Hãy tạo một món ăn hoàn chỉnh với:
1. **recipeName**: Tên món sáng tạo, điện ảnh
2. **narrativeStyle**: Chọn 1 trong (Comic Mode, Mystic Whisper, Action Rush, Ghibli Soft Dream, Cyberpunk Logic, Romance Mood, Drama Deep)
3. **story**: Câu chuyện nguồn gốc món ăn (2-3 đoạn văn)
4. **connection**: Lời bình của đạo diễn về ý nghĩa món ăn (1 câu sâu sắc)
5. **ingredients**: Mảng string đơn giản ["200g bột mì", "2 quả trứng", ...]
6. **instructions**: Mảng string kể như action scene ["Khi chảo bắt đầu phát ra tiếng xèo xèo...", ...]
7. **prepTime**: Ví dụ "15 phút"
8. **cookTime**: Ví dụ "30 phút"
9. **flavorProfile**: Đánh giá 0-10 cho từng chiều hương vị
10. **visualColors**: 3 mã màu HEX (VD: ["#FF0000", "#00FF00", "#0000FF"])
11. **platingGuide**: Mô tả cách trình bày như một cảnh phim
12. **pairing**: Gợi ý đồ uống hoặc món phụ (1 câu)
13. **musicRecommendation**: Tên bài nhạc phim hoặc OST phù hợp
14. **macros**: Ước tính dinh dưỡng (calories, protein, carbs, fat)
15. **origin**: Nguồn gốc món ăn (1-2 câu)

LƯU Ý:
- Ingredients & Instructions PHẢI là mảng STRING đơn giản, KHÔNG phải object
- Visual colors PHẢI là mã HEX hợp lệ (#RRGGBB)
- Narrative style phải khớp với mood của phim
"""

    # Initialize Gemini model
    model = genai.GenerativeModel(
        model_name='gemini-2.0-flash-exp',
        generation_config={
            "response_mime_type": "application/json",
            "response_schema": get_response_schema(),
            "temperature": 0.7 + (request.creativity / 100) * 0.3,  # 0.7-1.0 based on creativity
            "top_p": 0.95,
            "top_k": 40,
            "max_output_tokens": 8192,
        },
        system_instruction=SYSTEM_INSTRUCTION
    )

    try:
        # Generate - ĐÃ SỬA: Thêm await và dùng generate_content_async để không chặn server
        response = await model.generate_content_async(prompt)

        if not response.candidates:
            raise RuntimeError("AI đã chặn nội dung")

        raw_json = response.text.strip()
        if not raw_json:
            raise ValueError("AI trả về rỗng")

        # Parse JSON
        import json
        data = json.loads(raw_json)

        # Ensure color palette exists
        if not data.get('visualColors') or len(data['visualColors']) < 3:
            data['visualColors'] = generate_color_palette(
                data.get('narrativeStyle', 'Standard'),
                request.mood
            )

        # Validate & construct response
        result = CreativeChefResponse(**data)

        duration = (datetime.now() - start_time).total_seconds()
        logger.info(f"✅ Created: '{result.recipeName}' ({result.narrativeStyle}) in {duration:.2f}s")
        logger.info(f"🎨 Colors: {result.visualColors}")

        return result

    except json.JSONDecodeError as e:
        logger.error(f"JSON parse error: {e}")
        raise RuntimeError("AI trả về JSON không hợp lệ")
    except Exception as e:
        logger.error(f"Creation failed: {e}")
        raise RuntimeError(f"Không thể tạo món: {str(e)}")