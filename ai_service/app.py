import os
import io
import json
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
    title="CineTaste - AI Orchestration Service (Production-Grade)",
    description="Bộ não AI được tối ưu hóa cho hiệu năng và bảo trì, áp dụng các best practice.",
    version="4.0.0"
)

# --- 2. Định nghĩa các Model Dữ liệu (Pydantic) ---
# (Giữ nguyên cấu trúc)
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

# --- 3. Triển khai API Endpoints đã được Tối ưu ---

@app.post("/api/ai/analyze-dish", response_model=AnalyzeDishResponse, tags=["1. Chuyên gia Phân tích Ẩm thực"])
async def analyze_dish_from_image(
        image: UploadFile = File(...),
        context: Optional[str] = Form(None)
):
    try:
        image_bytes = await image.read()
        pil_image = Image.open(io.BytesIO(image_bytes))

        # === CẢI TIẾN: Thêm prefix 'models/' và hậu tố '-latest' ===
        model = genai.GenerativeModel('gemini-2.5-pro')

        prompt_context = context or "Không có"

        # === CẢI TIẾN: Tự động sinh schema từ Pydantic ===
        json_schema = AnalyzeDishResponse.schema_json(indent=2)

        prompt = f"""
        Bạn là một chuyên gia ẩm thực đa văn hóa. Hãy phân tích hình ảnh và thông tin bổ sung sau đây. 
        Nhiệm vụ của bạn là trả về một chuỗi JSON duy nhất, không có bất kỳ văn bản nào khác, tuân thủ nghiêm ngặt cấu trúc đã định.

        QUAN TRỌNG:
        1. Tất cả các giá trị số (calories, difficulty, thời gian, servings, step) phải là kiểu số NGUYÊN (integer), không phải chuỗi.
        2. Nếu bạn không thể nhận diện được món ăn từ hình ảnh, hãy đưa ra phỏng đoán hợp lý nhất có thể thay vì trả về lỗi.
        3. Nếu hình ảnh ko có 1 tý liên quan gì đến món ăn thì bạn nên đưa ra trả lời là  hình ảnh không phải món ăn nên bạn không thể tìm kiếm và đưa ra công thức liên quan được .
        Thông tin bổ sung từ người dùng: '{prompt_context}'

        Cấu trúc JSON bắt buộc phải tuân theo schema sau:
        {json_schema}
        """

        response = model.generate_content([prompt, pil_image])

        try:
            json_text = response.text.strip().replace("```json", "").replace("```", "")
            data = json.loads(json_text)
            return AnalyzeDishResponse(**data)
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail=f"AI trả về JSON không hợp lệ. Raw response: {response.text}")

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý AI: {str(e)}")


@app.post("/api/ai/modify-recipe", response_model=ModifiedRecipeResponse, tags=["2. Trợ lý Bếp AI"])
async def modify_recipe(request: ModifyRecipeRequest):
    try:
        # === CẢI TIẾN: Thêm prefix 'models/' và hậu tố '-latest' ===
        model = genai.GenerativeModel('gemini-2.0-flash')

        original_recipe_json = request.original_recipe.json(indent=2)

        prompt = f"""
        Bạn là một đầu bếp sáng tạo và thông minh. Dưới đây là một công thức nấu ăn gốc và một yêu cầu thay đổi.
        Nhiệm vụ của bạn là viết lại công thức mới một cách hợp lý dựa trên yêu cầu đó, đảm bảo công thức mới khả thi và ngon miệng.

        QUAN TRỌNG: 
        1. Giữ nguyên cấu trúc JSON của công thức gốc.
        2. Các giá trị số phải là kiểu số NGUYÊN.

        Yêu cầu thay đổi: '{request.modification_request}'

        Công thức gốc (định dạng JSON):
        {original_recipe_json}

        Hãy trả về một đối tượng JSON duy nhất có key là 'modified_recipe', bên trong chứa công thức đã được biến tấu. Không thêm bất kỳ văn bản nào khác.
        """

        response = model.generate_content(prompt)

        try:
            json_text = response.text.strip().replace("```json", "").replace("```", "")
            data = json.loads(json_text)
            return ModifiedRecipeResponse(**data)
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail=f"AI trả về JSON không hợp lệ. Raw response: {response.text}")

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý AI: {str(e)}")


@app.post("/api/ai/create-by-theme", response_model=AnalyzeDishResponse, tags=["3. Nhà Sáng tạo Món ăn"])
async def create_by_theme(request: CreateByThemeRequest):
    try:
        # === CẢI TIẾN: Thêm prefix 'models/' và hậu tố '-latest' ===
        model = genai.GenerativeModel('gemini-2.0-flash')

        # === CẢI TIẾN: Tự động sinh schema từ Pydantic ===
        json_schema = AnalyzeDishResponse.schema_json(indent=2)

        prompt = f"""
        Bạn là một nghệ sĩ ẩm thực với trí tưởng tượng phong phú, chuyên sáng tạo các món ăn lấy cảm hứng từ văn hóa đại chúng.
        Hãy sáng tạo một món ăn hoặc thức uống hoàn toàn mới dựa trên chủ đề sau: '{request.theme}'.
        Loại món ăn mong muốn là: '{request.dish_type}'.

        Hãy tưởng tượng ra món ăn đó và cung cấp một bộ thông tin hoàn chỉnh, chi tiết.
        Trả về kết quả dưới dạng một chuỗi JSON duy nhất, tuân thủ nghiêm ngặt cấu trúc schema sau đây:
        {json_schema}
        """

        response = model.generate_content(prompt)

        try:
            json_text = response.text.strip().replace("```json", "").replace("```", "")
            data = json.loads(json_text)
            return AnalyzeDishResponse(**data)
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail=f"AI trả về JSON không hợp lệ. Raw response: {response.text}")

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý AI: {str(e)}")


@app.post("/api/ai/critique-dish", response_model=CritiqueDishResponse, tags=["4. Giám khảo Mentor AI"])
async def critique_dish(
        image: UploadFile = File(...),
        dish_name: str = Form(...)
):
    try:
        image_bytes = await image.read()
        pil_image = Image.open(io.BytesIO(image_bytes))

        # === CẢI TIẾN: Thêm prefix 'models/' và hậu tố '-latest' ===
        model = genai.GenerativeModel('gemini-2.5-pro')

        # === CẢI TIẾN: Tối ưu prompt, tách logic và cấu trúc ===
        json_schema = CritiqueDishResponse.schema_json(indent=2)

        prompt = f"""
        Bạn là một người hướng dẫn nấu ăn thân thiện và tích cực (mentor). Hãy phân tích hình ảnh thành phẩm của món '{dish_name}' do người dùng nấu.
        
        Hãy viết nhận xét chi tiết theo các tiêu chí sau:
        - Ưu điểm: Bắt đầu bằng một lời khen ngợi chân thành về một điểm nổi bật bạn thấy.
        - Điểm cần cải thiện: Chỉ ra 1-2 điểm có thể cải thiện một cách nhẹ nhàng, mang tính xây dựng.
        - Lời khích lệ: Kết thúc bằng một lời động viên.
        - Điểm số: Chấm điểm thẩm mỹ trên thang điểm 10.

        Hãy trả về kết quả dưới dạng một chuỗi JSON duy nhất, tuân thủ cấu trúc schema sau:
        {json_schema}
        """

        response = model.generate_content([prompt, pil_image])

        try:
            json_text = response.text.strip().replace("```json", "").replace("```", "")
            data = json.loads(json_text)
            return CritiqueDishResponse(**data)
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail=f"AI trả về JSON không hợp lệ. Raw response: {response.text}")

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý AI: {str(e)}")


@app.get("/", tags=["Health Check"])
def health_check():
    # === CẢI TIẾN: Sửa lỗi cú pháp ===
    return {"status": "AI Service đang hoạt động!"}