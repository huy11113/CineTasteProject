# ai_service.py
from fastapi import FastAPI, UploadFile, Form
import google.generativeai as genai
import os

# Lấy API key từ biến môi trường
GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY")
genai.configure(api_key=GOOGLE_API_KEY)

# Khởi tạo FastAPI
app = FastAPI(title="CineTaste AI Service")

# Chọn model Gemini 2.5 (Flash hoặc Pro)
MODEL_ID = "gemini-2.5-flash"   # hoặc "gemini-2.5-pro"

# 1️⃣ Nhận diện món ăn từ ảnh
@app.post("/api/ai/identify-dish")
async def identify_dish(file: UploadFile):
    try:
        # Đọc dữ liệu ảnh
        img_bytes = await file.read()

        # Gửi request đến Gemini Vision
        model = genai.GenerativeModel(MODEL_ID)
        response = model.generate_content(
            ["Hãy cho biết món ăn trong ảnh này là gì?", img_bytes]
        )

        return {"dish_name": response.text}
    except Exception as e:
        return {"error": str(e)}

# 2️⃣ Biến tấu công thức
@app.post("/api/ai/modify-recipe")
async def modify_recipe(original_recipe: str = Form(...), request: str = Form(...)):
    try:
        model = genai.GenerativeModel(MODEL_ID)
        prompt = f"""
        Đây là công thức gốc:
        {original_recipe}

        Yêu cầu của người dùng: {request}

        Hãy trả về công thức mới đầy đủ, bao gồm nguyên liệu và các bước nấu.
        """
        response = model.generate_content(prompt)

        return {"modified_recipe": response.text}
    except Exception as e:
        return {"error": str(e)}

# 3️⃣ Phân tích hương vị từ nguyên liệu
@app.post("/api/ai/analyze-flavor")
async def analyze_flavor(ingredients: str = Form(...)):
    try:
        model = genai.GenerativeModel(MODEL_ID)
        prompt = f"""
        Đây là danh sách nguyên liệu: {ingredients}

        Hãy phân tích và cho biết hồ sơ hương vị (Flavor Profile): 
        - Vị chính (ngọt, mặn, chua, cay, béo, umami)
        - Phong cách ẩm thực có thể phù hợp
        - Món ăn gợi ý đi kèm
        """
        response = model.generate_content(prompt)

        return {"flavor_profile": response.text}
    except Exception as e:
        return {"error": str(e)}
