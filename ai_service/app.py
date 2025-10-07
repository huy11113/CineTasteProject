from fastapi import FastAPI, UploadFile, File, HTTPException
from pydantic import BaseModel
import google.generativeai as genai
import os
from dotenv import load_dotenv
from PIL import Image
import io

# Tải các biến môi trường từ file .env (chứa API key)
load_dotenv()

# Cấu hình Google AI SDK
GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')
if not GOOGLE_API_KEY:
    raise ValueError("Vui lòng thiết lập GOOGLE_API_KEY trong file .env")

genai.configure(api_key=GOOGLE_API_KEY)

# Khởi tạo ứng dụng FastAPI
app = FastAPI(
    title="CineTaste AI Service",
    description="Bộ não AI chuyên biệt cho việc nhận diện món ăn và xử lý công thức.",
    version="1.0.0"
)

# --- Các Pydantic Model để định nghĩa cấu trúc dữ liệu cho request/response ---

class ModifyRecipeRequest(BaseModel):
    original_recipe: str
    modification_request: str # Ví dụ: "biến thành món chay", "giảm 50% calo"

class ModifyRecipeResponse(BaseModel):
    modified_recipe: str

class IdentifyDishResponse(BaseModel):
    dish_name: str

# --- API Endpoints ---

@app.post("/api/ai/identify-dish", response_model=IdentifyDishResponse, tags=["AI Vision"])
async def identify_dish_from_image(image: UploadFile = File(...)):
    """
    Nhận một file ảnh, gọi đến Gemini Vision, và trả về tên món ăn.
    """
    try:
        # Đọc nội dung file ảnh
        contents = await image.read()
        pil_image = Image.open(io.BytesIO(contents))

        # Khởi tạo mô hình Gemini Pro Vision
        model = genai.GenerativeModel('gemini-pro-vision')

        # Thiết kế prompt
        prompt = "Hãy phân tích hình ảnh này và cho biết tên cụ thể của món ăn trong ảnh. Chỉ trả về tên món ăn, không thêm bất kỳ giải thích nào khác."

        # Gửi yêu cầu đến API
        response = model.generate_content([prompt, pil_image])

        # Xử lý và trả về kết quả
        dish_name = response.text.strip()

        return IdentifyDishResponse(dish_name=dish_name)

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý AI: {str(e)}")


@app.post("/api/ai/modify-recipe", response_model=ModifyRecipeResponse, tags=["AI Text"])
async def modify_recipe(request: ModifyRecipeRequest):
    """
    Nhận một công thức gốc và yêu cầu thay đổi, gọi đến Gemini Text, và trả về công thức đã biến tấu.
    """
    try:
        # Khởi tạo mô hình Gemini Pro
        model = genai.GenerativeModel('gemini-pro')

        # Thiết kế Prompt Template chi tiết
        prompt = f"""
        Bạn là một trợ lý bếp AI thông minh. Dưới đây là một công thức nấu ăn gốc và một yêu cầu thay đổi.
        Nhiệm vụ của bạn là viết lại công thức mới dựa trên yêu cầu đó. Giữ nguyên cấu trúc và định dạng của công thức gốc.

        **Công thức gốc:**
        {request.original_recipe}

        **Yêu cầu thay đổi:**
        {request.modification_request}

        **Công thức đã biến tấu:**
        """

        # Gửi yêu cầu đến API
        response = model.generate_content(prompt)

        return ModifyRecipeResponse(modified_recipe=response.text)

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý AI: {str(e)}")

@app.get("/", tags=["Health Check"])
def read_root():
    return {"message": "AI Service is running!"}