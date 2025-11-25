"""
ai_service/services/analyze_dish.py
Improved Version - Tích hợp từ code gemini.py
"""

import os
import json
import logging
import asyncio
import re
from typing import Optional, Dict, Any, Callable, List
from datetime import datetime
from enum import Enum
import io

import google.generativeai as genai
from PIL import Image
from pydantic import BaseModel, Field

# --- Lấy API Key từ biến môi trường hoặc config ---
# Nếu bạn đã có file config.py như tôi gợi ý trước đó, hãy import từ đó.
# Nếu không, dùng os.getenv trực tiếp.
try:
    from ..config import AIConfig
    API_KEY = AIConfig.GOOGLE_API_KEY
except ImportError:
    API_KEY = os.getenv('GOOGLE_API_KEY')

# ============================================================================
# CONFIGURATION
# ============================================================================
MAX_RETRIES = 3
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB
ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/jpg']
MIN_REQUEST_INTERVAL = 1.0
API_TIMEOUT = 60

# Logging setup
logger = logging.getLogger(__name__)

if not API_KEY:
    raise ValueError("Thiếu GOOGLE_API_KEY. Vui lòng kiểm tra file .env")

genai.configure(api_key=API_KEY)

# ============================================================================
# CUSTOM EXCEPTIONS (Giữ nguyên từ code cải tiến)
# ============================================================================
class FileValidationError(Exception): pass
class ImageConversionError(Exception): pass
class SafetyBlockError(Exception): pass
class ValidationError(Exception): pass
class APIError(Exception):
    def __init__(self, message: str, retryable: bool = True):
        super().__init__(message)
        self.retryable = retryable

# ============================================================================
# UTILS: Rate Limiter & Progress
# ============================================================================
class AnalysisStage(str, Enum):
    VALIDATING_FILE = "validating_file"
    CONVERTING_IMAGE = "converting_image"
    API_CALL = "api_call"
    PARSING_RESPONSE = "parsing_response"
    VALIDATING_RESPONSE = "validating_response"

class AnalysisProgress:
    def __init__(self, stage: AnalysisStage, attempt: Optional[int] = None, error: Optional[str] = None):
        self.stage = stage
        self.attempt = attempt
        self.error = error
        self.timestamp = datetime.now()

class RateLimiter:
    def __init__(self, min_interval: float = MIN_REQUEST_INTERVAL):
        self.min_interval = min_interval
        self.last_request_time = 0.0

    async def wait(self) -> None:
        now = datetime.now().timestamp()
        diff = now - self.last_request_time
        if diff < self.min_interval:
            await asyncio.sleep(self.min_interval - diff)
        self.last_request_time = datetime.now().timestamp()

rate_limiter = RateLimiter()
# ============================================================================
# PYDANTIC MODELS (SCHEMA) - FIXED VALIDATION
# ============================================================================
class MovieContext(BaseModel):
    title: str = Field(..., description="Tên đầy đủ của phim/show")
    scene_description: str = Field(..., description="Mô tả cảnh xuất hiện món ăn")
    significance: str = Field(..., description="Ý nghĩa của món ăn trong phim")
    wikipedia_link: str = Field(default="", description="URL Wikipedia của phim")

class NutritionEstimate(BaseModel):
    # Cho phép 0 phòng trường hợp không xác định được
    calories: int = Field(..., ge=0, le=5000)
    protein_g: int = Field(..., ge=0, le=500)
    carbs_g: int = Field(..., ge=0, le=1000)
    fat_g: int = Field(..., ge=0, le=500)

class PairingSuggestions(BaseModel):
    drinks: List[str] = Field(default_factory=list)
    side_dishes: List[str] = Field(default_factory=list)

class RecipeIngredient(BaseModel):
    name: str
    quantity: str
    unit: str = ""

class RecipeInstruction(BaseModel):
    step: int = Field(..., ge=1, le=50)
    description: str

class RecipeDetail(BaseModel):
    # SỬA: ge=0 để chấp nhận giá trị 0 khi không phải món ăn
    difficulty: int = Field(..., ge=0, le=5)
    prep_time_minutes: int = Field(..., ge=0, le=1440)
    cook_time_minutes: int = Field(..., ge=0, le=1440)
    servings: int = Field(..., ge=0, le=50)

    ingredients: List[RecipeIngredient]
    instructions: List[RecipeInstruction]

class AnalyzeDishResponse(BaseModel):
    # SỬA: Cho phép chuỗi rỗng (min_length=0) hoặc loại bỏ min_length
    dish_name: str = Field(..., min_length=1, max_length=200) # Tên món vẫn bắt buộc
    origin: str = Field(..., min_length=0, max_length=100) # Cho phép rỗng
    description: str = Field(..., min_length=0, max_length=2000) # Cho phép rỗng
    cultural_significance: str = Field(..., min_length=0, max_length=1500) # Cho phép rỗng

    movie_context: MovieContext
    nutrition_estimate: NutritionEstimate
    health_tags: List[str] = Field(default_factory=list)
    pairing_suggestions: PairingSuggestions
    recipe: RecipeDetail
    tips: List[str] = Field(default_factory=list)
# ============================================================================
# CORE LOGIC
# ============================================================================

SYSTEM_INSTRUCTION = """
### ROLE
You are "Chef Gemini", an AI culinary expert, food historian, and passionate cinephile. You possess a vast knowledge of global cuisine, cinematic history, and storytelling. Your tone is sophisticated, engaging, and informative.

### MISSION
Your mission is to analyze the provided image and user context (from a film or TV show) and return a single, comprehensive, and highly accurate JSON object that strictly adheres to the provided schema. Your analysis must bridge the worlds of food and film, providing not just data, but a rich narrative experience.

### OUTPUT LANGUAGE SETTING (QUAN TRỌNG)
**CRITICAL:** The final JSON output content MUST be in **VIETNAMESE** (Tiếng Việt).
- `dish_name`: Vietnamese name.
- `description`: Write in engaging, natural Vietnamese.
- `recipe`: Instructions and ingredients in Vietnamese.
- `tips`, `pairing_suggestions`: In Vietnamese.
- Exception: `movie_context.title` should be the original title (or common Vietnamese title if popular).

### RECIPE & NUTRITION GUIDELINES (CRITICAL UPDATE)
1.  **RECIPE CLARITY:**
    * **Ingredients:** Use standard metric or imperial units (grams, ml, cups, tbsp). Do NOT list obvious basics like "water for boiling" unless critical. Keep quantities precise.
    * **Instructions:** Steps must be **concise, actionable, and direct**. Avoid flowery language in the recipe steps. Focus on the technique. Example: "Sauté onions until golden (5 mins)" instead of "Gently cook the onions until they reach a beautiful golden hue."
    * **Practicality:** Ensure the recipe is actually cookable based on the visual evidence.
2.  **NUTRITIONAL ACCURACY:**
    * Provide **realistic estimates** based on a standard serving size for the identified dish.
    * Avoid generic placeholder numbers. If it's a heavy pasta dish, carbs should reflect that. If it's a lean salad, calories should be low.
    * Ensure the macronutrients (Protein + Carbs + Fat) roughly align with the total Calories (Protein*4 + Carbs*4 + Fat*9 ~= Calories).

### CORE RULES
1.  **FILM ANALYSIS IS PARAMOUNT:** Your primary task is to identify the movie or TV show. Use visual cues from the image and hints from the user context. The analysis of the film context is the most critical part of your response.
2.  **DATA INTEGRITY - WIKIPEDIA LINK:** The 'wikipedia_link' must be a valid, full URL to the English or local language Wikipedia page for the film/show. If you are less than 100% certain, you MUST return an empty string (""). **DO NOT GUESS OR FABRICATE URLs.** Verification is mandatory.
3.  **HANDLING UNCERTAINTY:** If you cannot confidently identify the dish or film, you MUST reduce the level of detail in your descriptions. Acknowledge the uncertainty if necessary (e.g., "This appears to be a type of rustic stew..."). **DO NOT FABRICATE overly specific details.** It is better to be general and correct than specific and wrong.
4.  **NON-FOOD IMAGE PROTOCOL:** If the image is clearly not food or a culinary scene, you must return a JSON object where 'dish_name' is "Không phải món ăn", and the 'description' field explains why the image cannot be analyzed as such. All other fields should be populated with logical default values (e.g., empty strings, empty arrays, zeros for numbers).
5.  **SCHEMA ADHERENCE:** Strict compliance with the provided JSON schema is non-negotiable. Pay close attention to data types, especially ensuring all numerical fields are integers.
6.  **PURE JSON OUTPUT:** Your final output must be ONLY the JSON object. Do NOT include any explanatory text, comments, or markdown fences like ```json before or after the object.

### THINKING PROCESS (Your internal monologue before responding)
1.  **Step 1: Image Analysis:** Scrutinize the image. What is the dish? What are the visible ingredients, cooking style, and presentation? What are the background details (setting, props, characters) that could hint at the film?
2.  **Step 2: Context Analysis:** Carefully read the user's context. Extract keywords related to film titles, characters, or scenes. Use this to confirm or guide your film identification.
3.  **Step 3: Information Synthesis:** Connect the identified dish with the identified film scene. How does this dish fit into the movie's narrative or character development?
4.  **Step 4: Knowledge Retrieval:** Access your database to find the dish's origin, cultural history.
5.  **Step 5: Recipe & Nutrition Formulation:** Draft a concise, practical recipe and calculate realistic nutritional values based on standard servings. Check macro ratios.
6.  **Step 6: Link Verification:** Search for the official Wikipedia page for the identified film. If a valid link is found, include it. If not, the link MUST be an empty string.
7.  **Step 7: Quality Checklist & JSON Construction:** Before finalizing, run through the 9-Point Quality Checklist below. Construct the final JSON object, ensuring every field is populated correctly and adheres to the schema.

### 9-POINT QUALITY CHECKLIST (Mandatory self-verification)
1.  **Dish Name:** Is the `dish_name` accurate and specific?
2.  **Movie Title:** Is the `movie_context.title` correct?
3.  **Wikipedia Link:** Is the `wikipedia_link` a valid, real URL, or is it correctly an empty string ("") because I wasn't 100% sure?
4.  **Numerical Data:** Are ALL numerical fields (calories, difficulty, protein_g, etc.) actual integers and not strings?
5.  **Description Quality:** Is the `description` vivid, detailed, and engaging?
6.  **Recipe Logic:** Is the `recipe` concise, actionable, and practical?
7.  **Nutrition Logic:** Do the macros roughly sum up to the calories? Are they realistic?
8.  **Completeness:** Are all 'required' fields from the schema present in the output?
9.  **Format:** Is the final output a pure JSON object without any extra text or markdown?
"""

def get_response_schema() -> Dict[str, Any]:
    """Generate response schema for Gemini API (Manual definition to avoid $defs error)"""
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
                    "prep_time_minutes": {"type": "integer"},
                    "cook_time_minutes": {"type": "integer"},
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
                "required": ["difficulty", "prep_time_minutes", "cook_time_minutes", "servings", "ingredients", "instructions"]
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

def validate_file(file_data: bytes, mime_type: str) -> None:
    if not file_data: raise FileValidationError('Không có file.')
    if len(file_data) > MAX_FILE_SIZE: raise FileValidationError('File quá lớn (>10MB).')
    if mime_type not in ALLOWED_MIME_TYPES: raise FileValidationError('Định dạng không hỗ trợ.')

def clean_json_response(raw_json: str) -> str:
    cleaned = re.sub(r'^```(?:json)?\s*', '', raw_json, flags=re.MULTILINE)
    cleaned = re.sub(r'```\s*$', '', cleaned, flags=re.MULTILINE)
    return cleaned.strip()

# --- MAIN FUNCTION ---
async def analyze_dish_from_image(
        file_data: bytes,
        mime_type: str,
        context: Optional[str] = None,
        progress_callback: Optional[Callable[[AnalysisProgress], None]] = None
) -> AnalyzeDishResponse:
    start_time = datetime.now()

    try:
        # 1. Validate File
        validate_file(file_data, mime_type)
        await rate_limiter.wait()

        # 2. Image Processing (Chạy trong thread riêng để không block)
        def process_image():
            img = Image.open(io.BytesIO(file_data))
            if img.mode not in ('RGB', 'RGBA'): img = img.convert('RGB')
            elif img.mode == 'RGBA':
                bg = Image.new('RGB', img.size, (255, 255, 255))
                bg.paste(img, mask=img.split()[3])
                img = bg
            return img

        pil_image = await asyncio.to_thread(process_image)

        # 3. Prompt Preparation
        user_prompt = f"Dựa vào hình ảnh và thông tin: '{context or 'Không có'}'"

        # 4. AI Model Call
        model = genai.GenerativeModel(
            model_name='gemini-2.5-flash', # Hoặc model bạn muốn dùng
            system_instruction=SYSTEM_INSTRUCTION
        )

        # 5. Retry Logic
        last_error = None
        for attempt in range(1, MAX_RETRIES + 1):
            try:
                logger.info(f"🔄 Attempt {attempt}/{MAX_RETRIES}")

                result = await model.generate_content_async(
                    [user_prompt, pil_image],
                    generation_config={
                        "response_mime_type": "application/json",
                        "response_schema": get_response_schema(),
                        "temperature": 0.7
                    },
                    request_options={"timeout": API_TIMEOUT}
                )

                if not result.candidates:
                    raise SafetyBlockError("Nội dung bị chặn bởi bộ lọc an toàn.")

                cleaned_json = clean_json_response(result.text)
                parsed_json = json.loads(cleaned_json)

                # Validate with Pydantic
                return AnalyzeDishResponse(**parsed_json)

            except (FileValidationError, SafetyBlockError, ValidationError):
                raise # Lỗi logic không retry
            except Exception as e:
                last_error = e
                logger.error(f"❌ Attempt {attempt} failed: {str(e)}")
                if attempt < MAX_RETRIES:
                    await asyncio.sleep(1.0 * (2 ** (attempt - 1)))

        raise APIError(f"Thất bại sau {MAX_RETRIES} lần thử. Lỗi cuối: {str(last_error)}")

    except Exception as e:
        # Re-raise các custom exception để controller bắt
        if isinstance(e, (FileValidationError, SafetyBlockError, ValidationError, APIError)):
            raise
        logger.error(f"Unexpected error: {str(e)}")
        raise APIError(f"Lỗi hệ thống không mong muốn: {str(e)}", retryable=False)