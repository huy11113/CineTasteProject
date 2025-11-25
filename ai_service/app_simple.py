"""
ai_service/app_simple.py
Version đơn giản để test - KHÔNG CẦN services folder
Dùng để kiểm tra xem Gemini API có hoạt động không
"""

import os
from fastapi import FastAPI, UploadFile, File, HTTPException
from PIL import Image
import io
import google.generativeai as genai

# Config
GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')
if not GOOGLE_API_KEY:
    raise ValueError("Thiếu GOOGLE_API_KEY trong .env")

genai.configure(api_key=GOOGLE_API_KEY)

app = FastAPI(title="AI Service - Simple Test")

@app.get("/")
def health():
    return {"status": "ok", "message": "Simple test version running"}

@app.post("/test-gemini")
async def test_gemini(image: UploadFile = File(...)):
    """Test xem Gemini có hoạt động không"""
    try:
        # Read image
        file_data = await image.read()
        pil_image = Image.open(io.BytesIO(file_data))

        # Simple prompt
        model = genai.GenerativeModel('gemini-2.0-flash-exp')
        response = model.generate_content([
            "Hãy mô tả món ăn trong ảnh này bằng tiếng Việt (1-2 câu ngắn gọn)",
            pil_image
        ])

        return {
            "success": True,
            "response": response.text,
            "model": "gemini-2.0-flash-exp"
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/test-json")
async def test_json(image: UploadFile = File(...)):
    """Test JSON response schema"""
    try:
        file_data = await image.read()
        pil_image = Image.open(io.BytesIO(file_data))

        # JSON schema
        schema = {
            "type": "object",
            "properties": {
                "dish_name": {"type": "string"},
                "description": {"type": "string"}
            },
            "required": ["dish_name", "description"]
        }

        model = genai.GenerativeModel(
            model_name='gemini-2.0-flash-exp',
            generation_config={
                "response_mime_type": "application/json",
                "response_schema": schema
            }
        )

        response = model.generate_content([
            "Trả về JSON với dish_name và description của món ăn",
            pil_image
        ])

        import json
        data = json.loads(response.text)

        return {
            "success": True,
            "data": data
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))