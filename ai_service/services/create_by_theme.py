"""
ai_service/services/create_by_theme.py
Creative Chef - Cinematic Storytelling Recipe Generator
(Updated: 8 New Personas - Optimized for Frontend)
"""

import logging
import os
import json
import re
import asyncio
from typing import Dict, Any, List
from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field, validator
import google.generativeai as genai

# --- CẤU HÌNH API KEY ---
try:
    from ..config import AIConfig
    API_KEY = AIConfig.GOOGLE_API_KEY
    MODEL_NAME = getattr(AIConfig, 'MODEL_SMART', 'gemini-2.5-flash')
except ImportError:
    API_KEY = os.getenv('GOOGLE_API_KEY')
    MODEL_NAME = 'gemini-2.5-flash'

if API_KEY:
    genai.configure(api_key=API_KEY)

logger = logging.getLogger(__name__)

# ============================================================================
# 1. ENUMS (CHUẨN HÓA THEO FRONTEND MỚI)
# ============================================================================

class NarrativeStyle(str, Enum):
    COMIC_MODE = "Comic Mode"           # Hài hước
    ACTION_RUSH = "Action Rush"         # Hành động
    ROMANCE_MOOD = "Romance Mood"       # Lãng mạn
    DRAMA_DEEP = "Drama Deep"           # Tâm lý
    HORROR_NIGHT = "Horror Night"       # Kinh dị
    CHEFS_TABLE = "Chef's Table"        # Tài liệu (Mới)
    ANIME_FEAST = "Anime Feast"         # Anime (Mới)
    TRAVEL_DISCOVERY = "Travel Discovery" # Khám phá (Mới)
    DEFAULT = "Standard"

# ============================================================================
# 2. MODELS
# ============================================================================

class CreateByThemeRequest(BaseModel):
    inspiration: str = Field(..., min_length=1, max_length=200, description="Tên phim/anime/chủ đề")
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

class CreateByThemeResponse(BaseModel):
    recipeName: str
    narrativeStyle: str
    story: str
    connection: str

    ingredients: List[str] = Field(default_factory=list)
    instructions: List[str] = Field(default_factory=list)

    prepTime: str
    cookTime: str

    flavorProfile: FlavorProfile
    visualColors: List[str]
    platingGuide: str

    pairing: str
    musicRecommendation: str
    macros: Macros
    origin: str = Field(default="")

# ============================================================================
# 3. UTILS (Bảng màu mới & Xử lý dữ liệu)
# ============================================================================

def generate_color_palette(style: str, mood: str) -> List[str]:
    """Tạo bảng màu hex dựa trên 8 style mới"""
    # Normalize: chữ thường, bỏ dấu cách, bỏ dấu gạch dưới, bỏ dấu nháy đơn (Chef's -> chefs)
    s = str(style).lower().replace(' ', '').replace('_', '').replace("'", "")
    m = str(mood).lower()

    color_map = {
        # Giữ nguyên
        'comicmode': ['#facc15', '#ef4444', '#2563eb'],      # Yellow/Red/Blue
        'actionrush': ['#7f1d1d', '#f97316', '#000000'],     # Red/Orange/Black
        'romancemood': ['#881337', '#ec4899', '#fb7185'],    # Pink/Rose
        'dramadeep': ['#451a03', '#78350f', '#171717'],      # Brown/Sepia
        'horrornight': ['#1a0000', '#4a0000', '#0a0a0a'],    # Dark Blood

        # Style Mới
        'chefstable': ['#1c1917', '#a8a29e', '#f5f5f4'],     # Stone/Elegant (Documentary)
        'animefeast': ['#ff6b35', '#f7b731', '#5f27cd'],     # Vibrant/Neon (Shokugeki)
        'traveldiscovery': ['#006d77', '#83c5be', '#ffddd2'], # Teal/Sand (Travel)
    }

    # Match style
    for key, colors in color_map.items():
        if key in s: return colors

    # Fallback theo Mood nếu style không khớp
    mood_map = {
        'horror': ['#2F0000', '#660000', '#0A0A0A'],
        'action': ['#FF6B35', '#F7931E', '#004E89'],
        'romance': ['#C2185B', '#F06292', '#F8BBD0'],
        'comedy': ['#FFC107', '#FF9800', '#03A9F4'],
        'documentary': ['#1c1917', '#a8a29e', '#f5f5f4'],
        'anime': ['#ff6b35', '#f7b731', '#5f27cd'],
    }

    for key, colors in mood_map.items():
        if key in m: return colors

    return ['#0f172a', '#334155', '#94a3b8'] # Default Slate

def clean_macros(macros_data: Dict[str, Any]) -> Dict[str, str]:
    """Sửa lỗi AI trả về khoảng giá trị (VD: '300-400' -> '300')"""
    cleaned = {}
    for key in ['calories', 'protein', 'carbs', 'fat']:
        value = str(macros_data.get(key, "0"))
        match = re.search(r'(\d+)', value)
        if match:
            num = match.group(1)
            cleaned[key] = num if key == 'calories' else f"{num}g"
        else:
            cleaned[key] = "0" if key == 'calories' else "0g"
    return cleaned

def translate_flavor_keys(flavor_data: Dict[str, int]) -> Dict[str, int]:
    """Chuyển đổi key tiếng Việt (từ AI) sang tiếng Anh (cho Frontend)"""
    map_vn_en = {
        'ngọt': 'sweet', 'chua': 'sour', 'cay': 'spicy',
        'mặn': 'umami', 'đậm đà': 'umami', 'umami': 'umami',
        'béo': 'richness', 'ngậy': 'richness', 'richness': 'richness'
    }
    new_profile = {"sweet": 0, "sour": 0, "spicy": 0, "umami": 0, "richness": 0}

    for k, v in flavor_data.items():
        k_lower = k.lower()
        if k_lower in new_profile:
            new_profile[k_lower] = v
        elif k_lower in map_vn_en:
            new_profile[map_vn_en[k_lower]] = v

    return new_profile

def clean_json_response(raw_json: str) -> str:
    """Làm sạch chuỗi JSON từ AI (loại bỏ markdown ```json)"""
    cleaned = re.sub(r'^```(?:json)?\s*', '', raw_json, flags=re.MULTILINE)
    cleaned = re.sub(r'```\s*$', '', cleaned, flags=re.MULTILINE)
    return cleaned.strip()

# ============================================================================
# 4. SYSTEM INSTRUCTION (CẬP NHẬT 8 PERSONAS MỚI)
# ============================================================================

SYSTEM_INSTRUCTION = """
### IDENTITY: CINEMATIC CULINARY STORYTELLER
You are a METHOD ACTOR AI - part chef, part screenwriter, part artist.

### MISSION
Transform a film/anime/theme into a complete sensory food experience.
This is NOT just a recipe - it's a NARRATIVE PERFORMANCE.

### 🎭 METHOD ACTING FRAMEWORK - 8 PERSONAS

You MUST embody one of these personas based on the film's genre/mood:

**1. 🎭 COMIC MODE (Comedy / Parody)**
- **Style:** Deadpool cooking, Asian comedy vlogger.
- **Tone:** Playful, breaks the 4th wall, sarcastic.
- **Language:** Slang, puns, talks directly to the reader.
- **Example:** "Chop that onion fast! Don't cry - save tears for the movie ending! This dish will make you forget your ex!"

**2. 🔥 ACTION RUSH (Action / High Speed)**
- **Style:** Gordon Ramsay, Fast & Furious trailer.
- **Tone:** Short, punchy sentences. Strong verbs (BLAST, SEAR, IGNITE).
- **Vibe:** Urgency, adrenaline, no hesitation.
- **Example:** "HIGH HEAT! Pan DOWN now! No hesitation - flavor needs EXPLOSION. Time is the enemy!"

**3. 🌹 ROMANCE MOOD (Romance / Sweet)**
- **Style:** K-Drama, French cinema, Before Sunrise.
- **Tone:** Sensual, sweet, deep emotions.
- **Metaphors:** Flavors as love (bittersweet, passionate, tender).
- **Example:** "Bitter chocolate melts into sweet cream, like hands clasped in rain. Season with tenderness."

**4. 🎬 DRAMA DEEP (Tragedy / Profound)**
- **Style:** Parasite, The Godfather, philosophical films.
- **Tone:** Thoughtful, heavy, meaningful.
- **Connection:** Food as a metaphor for life/struggle.
- **Example:** "This stew represents the chaos in Act 2 - everything crumbles yet hope lingers (sweet aftertaste)."

**5. 👻 HORROR NIGHT (Horror / Dark)**
- **Style:** Silence of the Lambs, Gothic horror.
- **Tone:** Eerie, suspenseful, unsettling but appetizing.
- **Imagery:** Dark colors, ominous sounds, blood metaphors.
- **Example:** "As the blade slices through flesh, crimson juices pool like a scene from a slasher film. The garlic hisses..."

**6. 📺 CHEF'S TABLE (Documentary / Art)**
- **Style:** Netflix Chef's Table, Jiro Dreams of Sushi.
- **Tone:** Reverent, focused on technique, ingredient origin.
- **Focus:** Craftsmanship, philosophy, history.
- **Example:** "The chef spent 40 years perfecting this broth. Every grain of salt is a prayer. Heat is not a number, it is intuition."

**7. 🍜 ANIME FEAST (Shokugeki / Food Wars)**
- **Style:** Food Wars, Toriko, exaggerated reactions.
- **Tone:** Hyper-energetic, visual effects, "foodgasm".
- **Description:** Explosive flavors, glowing food, clothes bursting.
- **Example:** "When the meat touches your tongue - BOOM! The universe explodes! Umami tsunami! Angels are singing!"

**8. 🌍 TRAVEL DISCOVERY (Travel / Culture)**
- **Style:** Anthony Bourdain, Street Food Asia.
- **Tone:** Curious, storytelling, connecting with locals.
- **Focus:** History, people, the story behind the stall.
- **Example:** "In a dark alley in Hanoi at 3 AM, Mrs. Hien still cooks pho like 50 years ago. This broth carries memories of war and love..."

### 🎯 OUTPUT RULES

**LANGUAGE:**
- Prompts: English (for AI comprehension)
- Output: NATURAL VIETNAMESE (như người Việt bản xứ nói, KHÔNG dịch máy)
- Story must be narrative-rich, not bland descriptions

**FLAVOR PROFILE:**
- Keys: "Ngọt", "Chua", "Cay", "Umami", "Béo" (Vietnamese)
- Values: Integers 0-10

**MACROS FORMAT:**
- **MUST BE SPECIFIC NUMBERS** (not ranges)
- Format: "380", "8g", "45g", "25g"

**INSTRUCTIONS:**
- Written as ACTION SCENES (not boring steps)
- Clear enough to actually cook from
- Balance narrative flair with practical clarity

**VISUAL COLORS:**
- MUST be valid HEX codes (#RRGGBB)
- Represent the film's color palette
"""

# ============================================================================
# 5. SCHEMA DEFINITION (CẬP NHẬT ENUM MỚI)
# ============================================================================

def get_response_schema() -> Dict[str, Any]:
    """JSON schema for Gemini structured output"""
    return {
        "type": "object",
        "properties": {
            "recipeName": {"type": "string", "description": "Creative name in Vietnamese"},
            "narrativeStyle": {
                "type": "string",
                "enum": [
                    "Comic Mode", "Action Rush", "Romance Mood",
                    "Drama Deep", "Horror Night", "Chef's Table",
                    "Anime Feast", "Travel Discovery"
                ],
                "description": "Choose ONE persona"
            },
            "story": {"type": "string", "description": "Compelling story in Vietnamese"},
            "connection": {"type": "string", "description": "Director's commentary"},
            "ingredients": {"type": "array", "items": {"type": "string"}},
            "instructions": {"type": "array", "items": {"type": "string"}},
            "prepTime": {"type": "string"},
            "cookTime": {"type": "string"},
            "flavorProfile": {
                "type": "object",
                "properties": {
                    "Ngọt": {"type": "integer"}, "Chua": {"type": "integer"},
                    "Cay": {"type": "integer"}, "Umami": {"type": "integer"},
                    "Béo": {"type": "integer"}
                },
                "required": ["Ngọt", "Chua", "Cay", "Umami", "Béo"]
            },
            "visualColors": {"type": "array", "items": {"type": "string"}},
            "platingGuide": {"type": "string"},
            "pairing": {"type": "string"},
            "musicRecommendation": {"type": "string"},
            "macros": {
                "type": "object",
                "properties": {"calories": {"type": "string"}, "protein": {"type": "string"}, "carbs": {"type": "string"}, "fat": {"type": "string"}}
            },
            "origin": {"type": "string"}
        },
        "required": ["recipeName", "narrativeStyle", "story", "connection", "ingredients", "instructions", "flavorProfile", "visualColors", "macros"]
    }

# ============================================================================
# 6. MAIN FUNCTION
# ============================================================================

async def create_by_theme(request: CreateByThemeRequest) -> CreateByThemeResponse:
    start_time = datetime.now()
    logger.info(f"🎬 Creating dish: {request.inspiration} | Mood: {request.mood}")

    # Prompt xây dựng bằng tiếng Anh để AI hiểu rõ ngữ cảnh, output tiếng Việt
    prompt = f"""
    🎬 **CREATIVE MISSION**
    **INSPIRATION:** {request.inspiration}
    **MOOD/GENRE:** {request.mood}
    **CREATIVITY LEVEL:** {request.creativity}/100
    **COOKING TIME:** {request.time}
    **DIFFICULTY:** {request.difficulty}
    **DIETARY:** {request.diet}
    {"**AVAILABLE INGREDIENTS:** " + request.ingredients if request.ingredients else ""}

    GENERATE A JSON RECIPE IN VIETNAMESE.
    - Style: Pick ONE of the 8 personas (Chef's Table, Anime Feast, etc.) matching the mood.
    - Story: Deep storytelling, connected to the movie scene.
    - Macros: Specific numbers only.
    """

    model = genai.GenerativeModel(
        model_name=MODEL_NAME, # Sử dụng config từ file cấu hình
        generation_config={
            "response_mime_type": "application/json",
            "response_schema": get_response_schema(),
            "temperature": 0.8, # Tăng nhẹ creativity cho persona mới
        },
        system_instruction=SYSTEM_INSTRUCTION
    )

    # Retry Logic
    MAX_RETRIES = 2
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            response = await model.generate_content_async(prompt, request_options={"timeout": 60})

            if not response.candidates:
                raise RuntimeError("AI blocked content")

            # Clean & Parse
            cleaned_json = clean_json_response(response.text)
            data = json.loads(cleaned_json)

            # 1. Fallback màu sắc (logic mới)
            if not data.get('visualColors') or len(data['visualColors']) < 3:
                data['visualColors'] = generate_color_palette(data.get('narrativeStyle', 'Standard'), request.mood)

            # 2. Fix Macros
            if 'macros' in data:
                data['macros'] = clean_macros(data['macros'])

            # 3. Translate Flavor Keys (Ngọt -> sweet)
            if 'flavorProfile' in data:
                data['flavorProfile'] = translate_flavor_keys(data['flavorProfile'])

            result = CreateByThemeResponse(**data)

            duration = (datetime.now() - start_time).total_seconds()
            logger.info(f"✅ Cut! Print it! Generated '{result.recipeName}' ({result.narrativeStyle}) in {duration:.2f}s")

            return result

        except Exception as e:
            logger.error(f"Attempt {attempt} failed: {str(e)}")
            if attempt < MAX_RETRIES:
                await asyncio.sleep(1)
            else:
                raise RuntimeError(f"Lỗi AI: {str(e)}")