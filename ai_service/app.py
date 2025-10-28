import os
import io
import json
import asyncio # Thêm import asyncio
from functools import lru_cache # Thêm import lru_cache
from dotenv import load_dotenv
from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from pydantic import BaseModel, Field
from typing import List, Optional
from PIL import Image
import google.generativeai as genai

# --- 1. Cấu hình ban đầu ---
load_dotenv()
GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')
if not GOOGLE_API_KEY:
    raise ValueError("Vui lòng thiết lập GOOGLE_API_KEY trong file .env")
genai.configure(api_key=GOOGLE_API_KEY)

app = FastAPI(
    title="CineTaste - AI Service (Optimized)",
    description="Bộ não AI được tối ưu hóa cho hiệu năng và bảo trì.",
    version="5.0.0" # Cập nhật version
)

# --- 2. Tối ưu hóa: Caching Model & Warm-up ---
@lru_cache(maxsize=5) # Cache tối đa 5 model instances
def get_model(model_name: str):
    """Cache models để tránh khởi tạo lại mỗi request. Tối ưu hiệu năng."""
    print(f"[*] Initializing or getting cached Gemini model: {model_name}")
    # Bạn có thể thêm cấu hình generation_config hoặc safety_settings ở đây nếu cần
    # generation_config = {...}
    # safety_settings = [...]
    # return genai.GenerativeModel(model_name, generation_config=generation_config, safety_settings=safety_settings)
    return genai.GenerativeModel(model_name)

@app.on_event("startup")
async def startup_event():
    """Warm-up các model chính khi service khởi động để giảm độ trễ cho request đầu tiên."""
    print("[*] Warming up AI models...")
    loop = asyncio.get_event_loop()
    try:
        # Sử dụng model bạn đã chỉ định ban đầu
        # Chạy blocking I/O trong executor để không chặn event loop
        await loop.run_in_executor(None, get_model, 'gemini-2.5-pro') # <-- SỬA LẠI MODEL
        await loop.run_in_executor(None, get_model, 'gemini-2.5-flash') # <-- SỬA LẠI MODEL
        # Thêm các model khác nếu cần warm-up
        print("✅ AI models pre-loaded successfully.")
    except Exception as e:
        print(f"⚠️ Model pre-load warning: {e}")
        # Cân nhắc: Có thể thêm logic xử lý lỗi nghiêm trọng hơn ở đây nếu cần

# --- 3. Tối ưu hóa: Xử lý Ảnh ---
async def optimize_image(image: UploadFile, max_size: tuple = (1024, 1024)) -> Image.Image:
    """Tối ưu kích thước và định dạng ảnh trước khi gửi đến AI."""
    try:
        image_bytes = await image.read()
        pil_image = Image.open(io.BytesIO(image_bytes))

        # Chuyển đổi sang RGB nếu cần (loại bỏ kênh alpha hoặc xử lý ảnh grayscale)
        if pil_image.mode not in ('RGB', 'RGBA'):
            pil_image = pil_image.convert('RGB')
        elif pil_image.mode == 'RGBA':
            # Tạo nền trắng và dán ảnh RGBA lên để loại bỏ kênh alpha
            background = Image.new('RGB', pil_image.size, (255, 255, 255))
            background.paste(pil_image, mask=pil_image.split()[3]) # 3 is the alpha channel
            pil_image = background

        # Resize ảnh nếu kích thước vượt quá giới hạn max_size
        original_size = pil_image.size
        if original_size[0] > max_size[0] or original_size[1] > max_size[1]:
            pil_image.thumbnail(max_size, Image.Resampling.LANCZOS)
            print(f"📐 Resized image from {original_size} to {pil_image.size}")

        return pil_image
    except Exception as e:
        print(f"🚨 Error optimizing image: {str(e)}")
        raise HTTPException(status_code=400, detail=f"Không thể xử lý ảnh: {str(e)}")

# --- 4. Định nghĩa các Model Dữ liệu (Pydantic) ---
# (Giữ nguyên các Pydantic model của bạn: NutritionEstimate, PairingSuggestions, ...)
class NutritionEstimate(BaseModel):
    calories: int
    protein: str
    carbs: str
    fat: str

class PairingSuggestions(BaseModel):
    drinks: List[str]
    side_dishes: List[str]

class RecipeIngredient(BaseModel):
    name: str
    quantity: str
    unit: str

class RecipeInstruction(BaseModel):
    step: int
    description: str

class RecipeDetail(BaseModel):
    difficulty: int = Field(..., ge=1, le=5)
    prep_time_minutes: int
    cook_time_minutes: int
    servings: int
    ingredients: List[RecipeIngredient]
    instructions: List[RecipeInstruction]

class AnalyzeDishResponse(BaseModel):
    dish_name: str
    origin: str
    description: str
    nutrition_estimate: NutritionEstimate
    health_tags: List[str]
    pairing_suggestions: PairingSuggestions
    recipe: RecipeDetail
    tips: List[str]

class ModifyRecipeRequest(BaseModel):
    original_recipe: RecipeDetail
    modification_request: str

class ModifiedRecipeResponse(BaseModel):
    modified_recipe: RecipeDetail

class CreateByThemeRequest(BaseModel):
    theme: str
    dish_type: str

class CritiqueDishResponse(BaseModel):
    critique: str
    score: float = Field(..., ge=0, le=10)
    suggestions: List[str]


# --- 5. Triển khai API Endpoints đã được Tối ưu ---

@app.post("/api/ai/analyze-dish", response_model=AnalyzeDishResponse, tags=["1. Chuyên gia Phân tích Ẩm thực"])
async def analyze_dish_from_image(
        image: UploadFile = File(...),
        context: Optional[str] = Form(None)
):
    """
    Phân tích món ăn từ hình ảnh, trả về thông tin chi tiết và công thức.
    Sử dụng model caching và tối ưu hóa ảnh.
    """
    loop = asyncio.get_event_loop()
    try:
        # Tối ưu ảnh trước khi gửi đến AI
        pil_image = await optimize_image(image)

        # Sử dụng model đã được cache
        model = get_model('gemini-2.5-pro') # <-- SỬA LẠI MODEL

        prompt_context = context or "Không có"
        json_schema = AnalyzeDishResponse.schema_json(indent=2)

        # Giữ nguyên prompt chi tiết của bạn
        prompt = f"""
        Bạn là một chuyên gia ẩm thực đa văn hóa. Hãy phân tích hình ảnh và thông tin bổ sung sau đây.
        Nhiệm vụ của bạn là trả về một chuỗi JSON duy nhất, không có bất kỳ văn bản nào khác, tuân thủ nghiêm ngặt cấu trúc đã định.

        QUAN TRỌNG:
        1. Tất cả các giá trị số (calories, difficulty, thời gian, servings, step) phải là kiểu số NGUYÊN (integer), không phải chuỗi.
        2. Nếu bạn không thể nhận diện được món ăn từ hình ảnh, hãy đưa ra phỏng đoán hợp lý nhất có thể thay vì trả về lỗi.
        3. Nếu hình ảnh không liên quan gì đến món ăn, hãy trả về JSON với dish_name là "Không phải món ăn" và description giải thích rõ.
        Thông tin bổ sung từ người dùng: '{prompt_context}'

        Cấu trúc JSON bắt buộc phải tuân theo schema sau:
        {json_schema}
        """

        # Chạy lệnh gọi API Gemini (blocking I/O) trong thread executor
        response = await loop.run_in_executor(None, model.generate_content, [prompt, pil_image])

        # Xử lý kết quả JSON
        try:
            # Loại bỏ ```json ``` nếu có
            json_text = response.text.strip()
            if json_text.startswith("```json"):
                json_text = json_text[7:]
            if json_text.endswith("```"):
                json_text = json_text[:-3]

            data = json.loads(json_text.strip())
            return AnalyzeDishResponse(**data)
        except json.JSONDecodeError as json_error:
            print(f"🚨 AI JSON Decode Error. Raw response: {response.text}\nError: {json_error}")
            raise HTTPException(status_code=500, detail=f"AI trả về JSON không hợp lệ.")
        except Exception as pydantic_error: # Bắt lỗi validation của Pydantic
            print(f"🚨 Pydantic Validation Error. Data: {data}\nError: {pydantic_error}")
            raise HTTPException(status_code=500, detail=f"Dữ liệu AI trả về không khớp cấu trúc: {pydantic_error}")

    except HTTPException as http_exc: # Re-raise HTTPException để giữ nguyên status code (vd: lỗi optimize_image)
        raise http_exc
    except Exception as e:
        print(f"🚨 Unexpected Error in analyze_dish: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống khi xử lý AI: {str(e)}")


@app.post("/api/ai/modify-recipe", response_model=ModifiedRecipeResponse, tags=["2. Trợ lý Bếp AI"])
async def modify_recipe(request: ModifyRecipeRequest):
    """
    Biến tấu công thức gốc dựa trên yêu cầu người dùng.
    Sử dụng model caching.
    """
    loop = asyncio.get_event_loop()
    try:
        # Sử dụng model đã được cache
        model = get_model('gemini-2.5-flash') # <-- SỬA LẠI MODEL

        original_recipe_json = request.original_recipe.model_dump_json(indent=2) # Sử dụng model_dump_json thay vì .json()

        # Giữ nguyên prompt chi tiết của bạn
        prompt = f"""
        Bạn là một đầu bếp sáng tạo và thông minh. Dưới đây là một công thức nấu ăn gốc và một yêu cầu thay đổi.
        Nhiệm vụ của bạn là viết lại công thức mới một cách hợp lý dựa trên yêu cầu đó, đảm bảo công thức mới khả thi và ngon miệng.

        QUAN TRỌNG:
        1. Giữ nguyên cấu trúc JSON của công thức gốc trong key 'modified_recipe'.
        2. Các giá trị số phải là kiểu số NGUYÊN.

        Yêu cầu thay đổi: '{request.modification_request}'

        Công thức gốc (định dạng JSON):
        {original_recipe_json}

        Hãy trả về một đối tượng JSON duy nhất có key là 'modified_recipe', bên trong chứa công thức đã được biến tấu tuân thủ schema của RecipeDetail. Không thêm bất kỳ văn bản nào khác ngoài JSON object đó.
        """

        # Chạy lệnh gọi API Gemini trong thread executor
        response = await loop.run_in_executor(None, model.generate_content, prompt)

        try:
            json_text = response.text.strip()
            if json_text.startswith("```json"):
                json_text = json_text[7:]
            if json_text.endswith("```"):
                json_text = json_text[:-3]

            data = json.loads(json_text.strip())
            # Kiểm tra xem có key 'modified_recipe' không
            if 'modified_recipe' not in data:
                print(f"🚨 AI Modify Error: Missing 'modified_recipe' key. Raw: {response.text}")
                raise HTTPException(status_code=500, detail="AI trả về thiếu key 'modified_recipe'.")

            return ModifiedRecipeResponse(**data) # Pydantic sẽ validate cấu trúc bên trong modified_recipe
        except json.JSONDecodeError as json_error:
            print(f"🚨 AI JSON Decode Error. Raw response: {response.text}\nError: {json_error}")
            raise HTTPException(status_code=500, detail=f"AI trả về JSON không hợp lệ.")
        except Exception as pydantic_error:
            print(f"🚨 Pydantic Validation Error. Data: {data}\nError: {pydantic_error}")
            raise HTTPException(status_code=500, detail=f"Dữ liệu AI trả về không khớp cấu trúc: {pydantic_error}")

    except Exception as e:
        print(f"🚨 Unexpected Error in modify_recipe: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống khi xử lý AI: {str(e)}")


@app.post("/api/ai/create-by-theme", response_model=AnalyzeDishResponse, tags=["3. Nhà Sáng tạo Món ăn"])
async def create_by_theme(request: CreateByThemeRequest):
    """
    Sáng tạo công thức mới dựa trên chủ đề và loại món ăn.
    Sử dụng model caching. Trả về cấu trúc AnalyzeDishResponse.
    """
    loop = asyncio.get_event_loop()
    try:
        # Sử dụng model đã được cache
        model = get_model('gemini-2.5-flash') # <-- SỬA LẠI MODEL

        json_schema = AnalyzeDishResponse.schema_json(indent=2)

        # Giữ nguyên prompt chi tiết của bạn
        prompt = f"""
        Bạn là một nghệ sĩ ẩm thực với trí tưởng tượng phong phú, chuyên sáng tạo các món ăn lấy cảm hứng từ văn hóa đại chúng.
        Hãy sáng tạo một món ăn hoặc thức uống hoàn toàn mới dựa trên chủ đề sau: '{request.theme}'.
        Loại món ăn mong muốn là: '{request.dish_type}'.

        Hãy tưởng tượng ra món ăn đó và cung cấp một bộ thông tin hoàn chỉnh, chi tiết bao gồm cả công thức.
        Trả về kết quả dưới dạng một chuỗi JSON duy nhất, tuân thủ nghiêm ngặt cấu trúc schema AnalyzeDishResponse sau đây:
        {json_schema}
        """

        # Chạy lệnh gọi API Gemini trong thread executor
        response = await loop.run_in_executor(None, model.generate_content, prompt)

        try:
            json_text = response.text.strip()
            if json_text.startswith("```json"):
                json_text = json_text[7:]
            if json_text.endswith("```"):
                json_text = json_text[:-3]

            data = json.loads(json_text.strip())
            return AnalyzeDishResponse(**data)
        except json.JSONDecodeError as json_error:
            print(f"🚨 AI JSON Decode Error. Raw response: {response.text}\nError: {json_error}")
            raise HTTPException(status_code=500, detail=f"AI trả về JSON không hợp lệ.")
        except Exception as pydantic_error:
            print(f"🚨 Pydantic Validation Error. Data: {data}\nError: {pydantic_error}")
            raise HTTPException(status_code=500, detail=f"Dữ liệu AI trả về không khớp cấu trúc: {pydantic_error}")

    except Exception as e:
        print(f"🚨 Unexpected Error in create_by_theme: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống khi xử lý AI: {str(e)}")


@app.post("/api/ai/critique-dish", response_model=CritiqueDishResponse, tags=["4. Giám khảo Mentor AI"])
async def critique_dish(
        image: UploadFile = File(...),
        dish_name: str = Form(...)
):
    """
    Nhận xét, chấm điểm và đưa ra gợi ý cải thiện món ăn từ hình ảnh.
    Sử dụng model caching và tối ưu hóa ảnh.
    """
    loop = asyncio.get_event_loop()
    try:
        # Tối ưu ảnh
        pil_image = await optimize_image(image)

        # Sử dụng model đã được cache
        model = get_model('gemini-2.5-pro') # <-- SỬA LẠI MODEL

        json_schema = CritiqueDishResponse.schema_json(indent=2)

        # Giữ nguyên prompt chi tiết của bạn
        prompt = f"""
        Bạn là một người hướng dẫn nấu ăn thân thiện và tích cực (mentor). Hãy phân tích hình ảnh thành phẩm của món '{dish_name}' do người dùng nấu.

        Hãy viết nhận xét chi tiết theo các tiêu chí sau:
        - Ưu điểm: Bắt đầu bằng một lời khen ngợi chân thành về một điểm nổi bật bạn thấy.
        - Điểm cần cải thiện: Chỉ ra 1-2 điểm có thể cải thiện một cách nhẹ nhàng, mang tính xây dựng.
        - Lời khích lệ: Kết thúc bằng một lời động viên.
        - Điểm số: Chấm điểm thẩm mỹ và độ hấp dẫn dựa trên hình ảnh trên thang điểm 10 (cho phép số lẻ, ví dụ 8.5).

        Hãy trả về kết quả dưới dạng một chuỗi JSON duy nhất, tuân thủ cấu trúc schema CritiqueDishResponse sau:
        {json_schema}
        """

        # Chạy lệnh gọi API Gemini trong thread executor
        response = await loop.run_in_executor(None, model.generate_content, [prompt, pil_image])

        try:
            json_text = response.text.strip()
            if json_text.startswith("```json"):
                json_text = json_text[7:]
            if json_text.endswith("```"):
                json_text = json_text[:-3]

            data = json.loads(json_text.strip())
            return CritiqueDishResponse(**data)
        except json.JSONDecodeError as json_error:
            print(f"🚨 AI JSON Decode Error. Raw response: {response.text}\nError: {json_error}")
            raise HTTPException(status_code=500, detail=f"AI trả về JSON không hợp lệ.")
        except Exception as pydantic_error:
            print(f"🚨 Pydantic Validation Error. Data: {data}\nError: {pydantic_error}")
            raise HTTPException(status_code=500, detail=f"Dữ liệu AI trả về không khớp cấu trúc: {pydantic_error}")

    except HTTPException as http_exc:
        raise http_exc
    except Exception as e:
        print(f"🚨 Unexpected Error in critique_dish: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống khi xử lý AI: {str(e)}")

# --- 6. Health Check Endpoint ---
@app.get("/", tags=["Health Check"])
def health_check():
    """Kiểm tra trạng thái hoạt động của AI Service."""
    return {"status": "healthy", "service": "ai-service", "version": app.version}